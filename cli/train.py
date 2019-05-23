import argparse
import collections
import os
import pickle
import re
import shutil

import torch

from cli import CLIArgsException, StatefulActivity, activitystep, argv_has
from cli.mmt import MMT_FAIRSEQ_USER_DIR
from cli.utils import osutils
from cli.utils.osutils import ShellError


def _last_n_checkpoints(path, n, regex):
    pt_regexp = re.compile(regex)
    files = os.listdir(path)

    entries = []
    for f in files:
        m = pt_regexp.fullmatch(f)
        if m is not None:
            sort_key = int(m.group(1))
            entries.append((sort_key, m.group(0)))

    return [os.path.join(path, x[1]) for x in sorted(entries, reverse=True)[:n]]


class TrainActivity(StatefulActivity):
    def __init__(self, args, extra_argv=None, wdir=None, log_file=None, start_step=None, delete_on_exit=True):
        super().__init__(args, extra_argv, wdir, log_file, start_step, delete_on_exit)

        if args.resume:
            self.state.step_no = self._index_of_step('train_nn') - 1

    @activitystep('Train neural network')
    def train_nn(self):
        self.state.nn_path = self.wdir('nn_model')

        last_ckpt_path = os.path.join(self.state.nn_path, 'checkpoint_last.pt')
        if not os.path.isfile(last_ckpt_path) and self.args.init_model is not None:
            shutil.copy(self.args.init_model, last_ckpt_path)

        # Create command
        cmd = ['fairseq-train', self.args.data_path, '--save-dir', self.state.nn_path, '--task', 'mmt_translation',
               '--user-dir', MMT_FAIRSEQ_USER_DIR, '--share-all-embeddings', '--no-progress-bar']
        cmd += self.extra_argv

        # Create environment
        env = None
        if self.args.gpus is not None:
            env = os.environ.copy()
            env['CUDA_VISIBLE_DEVICES'] = ','.join([str(gpu) for gpu in self.args.gpus])

        # Start process
        tensorboard = None

        if self.args.tensorboard_port is not None:
            tensorboard_logdir = self.state.tensorboard_logdir = self.wdir('tensorboard_logdir')
            tensorboard_log = open(os.path.join(self.state.tensorboard_logdir, 'server.log'), 'wb')
            tensorboard_cmd = ['tensorboard', '--logdir', tensorboard_logdir, '--port', str(self.args.tensorboard_port)]
            tensorboard = osutils.shell_exec(tensorboard_cmd,
                                             stderr=tensorboard_log, stdout=tensorboard_log, background=True)

            cmd += ['--tensorboard-logdir', tensorboard_logdir]

        process = osutils.shell_exec(cmd, stderr=self.log_fobj, stdout=self.log_fobj, background=True, env=env)

        try:
            return_code = process.wait()

            if return_code != 0:
                raise ShellError(' '.join(cmd), return_code)
        except KeyboardInterrupt:
            process.terminate()
        finally:
            if tensorboard is not None:
                tensorboard.terminate()

    @activitystep('Averaging checkpoints')
    def avg_checkpoints(self):
        checkpoints = _last_n_checkpoints(self.state.nn_path, self.args.num_checkpoints, r'checkpoint_\d+_(\d+)\.pt')
        if len(checkpoints) == 0:
            # by epoch
            checkpoints = _last_n_checkpoints(self.state.nn_path, self.args.num_checkpoints, r'checkpoint(\d+)\.pt')

        if len(checkpoints) == 0:
            raise ValueError('no checkpoints found in ' + self.state.nn_path)

        self._logger.info('Averaging checkpoints: ' + str(checkpoints))

        with open(os.path.join(self.args.data_path, 'decode_lengths.bin'), 'rb') as f:
            decode_lengths = pickle.load(f)

        # Average checkpoints
        params_dict = collections.OrderedDict()
        params_keys = None
        avg_state = None

        for f in checkpoints:
            state = torch.load(f, map_location=lambda s, _: torch.serialization.default_restore_location(s, 'cpu'))
            # Copies over the settings from the first checkpoint
            if avg_state is None:
                avg_state = state

            model_params = state['model']

            model_params_keys = list(model_params.keys())
            if params_keys is None:
                params_keys = model_params_keys
            elif params_keys != model_params_keys:
                raise KeyError(
                    'For checkpoint {}, expected list of params: {}, '
                    'but found: {}'.format(f, params_keys, model_params_keys)
                )

            for k in params_keys:
                if k not in params_dict:
                    params_dict[k] = []
                p = model_params[k]
                if isinstance(p, torch.HalfTensor):
                    p = p.float()
                params_dict[k].append(p)

        averaged_params = collections.OrderedDict()
        # v should be a list of torch Tensor.
        for k, v in params_dict.items():
            summed_v = None
            for x in v:
                summed_v = summed_v + x if summed_v is not None else x
            averaged_params[k] = summed_v / len(v)

        avg_state['model'] = averaged_params
        avg_state['decode_stats'] = decode_lengths

        # Save model
        os.makedirs(self.args.output_path, exist_ok=True)
        torch.save(avg_state, os.path.join(self.args.output_path, 'model.pt'))
        shutil.copy(os.path.join(self.args.data_path, 'model.vcb'), os.path.join(self.args.output_path, 'model.vcb'))


def parse_extra_argv(parser, extra_argv):
    for reserved_opt in ['--save-dir', '--user-dir', '--task', '--no-progress-bar', '--share-all-embeddings',
                         '--tensorboard-logdir']:
        if reserved_opt in extra_argv:
            raise CLIArgsException(parser, 'overriding option "%s" is not allowed' % reserved_opt)

    cmd_extra_args = extra_argv[:]

    if not argv_has(cmd_extra_args, '-a', '--arch'):
        cmd_extra_args.extend(['--arch', 'transformer_mmt_base'])
    if not argv_has(cmd_extra_args, '--clip-norm'):
        cmd_extra_args.extend(['--clip-norm', '0.0'])
    if not argv_has(cmd_extra_args, '--label-smoothing'):
        cmd_extra_args.extend(['--label-smoothing', '0.1'])
    if not argv_has(cmd_extra_args, '--attention-dropout'):
        cmd_extra_args.extend(['--attention-dropout', '0.1'])
    if not argv_has(cmd_extra_args, '--dropout'):
        cmd_extra_args.extend(['--dropout', '0.3'])
    if not argv_has(cmd_extra_args, '--wd', '--weight-decay'):
        cmd_extra_args.extend(['--weight-decay', '0.0'])
    if not argv_has(cmd_extra_args, '--criterion'):
        cmd_extra_args.extend(['--criterion', 'label_smoothed_cross_entropy'])

    if not argv_has(cmd_extra_args, '--optimizer'):
        cmd_extra_args.extend(['--optimizer', 'adam'])
        if not argv_has(cmd_extra_args, '--adam-betas'):
            cmd_extra_args.extend(['--adam-betas', '(0.9, 0.98)'])

    if not argv_has(cmd_extra_args, '--log-interval'):
        cmd_extra_args.extend(['--log-interval', '100'])

    if not argv_has(cmd_extra_args, '--lr', '--learning-rate'):
        cmd_extra_args.extend(['--lr', '0.0001'])
    if not argv_has(cmd_extra_args, '--lr-scheduler'):
        cmd_extra_args.extend(['--lr-scheduler', 'inverse_sqrt'])
    if not argv_has(cmd_extra_args, '--min-lr'):
        cmd_extra_args.extend(['--min-lr', '1e-09'])
    if not argv_has(cmd_extra_args, '--warmup-init-lr'):
        cmd_extra_args.extend(['--warmup-init-lr', '1e-07'])
    if not argv_has(cmd_extra_args, '--warmup-updates'):
        cmd_extra_args.extend(['--warmup-updates', '4000'])

    if not argv_has(cmd_extra_args, '--max-tokens'):
        cmd_extra_args.extend(['--max-tokens', '1536'])

    if not argv_has(cmd_extra_args, '--save-interval-updates'):
        cmd_extra_args.extend(['--save-interval-updates', '2000'])
    if not argv_has(cmd_extra_args, '--keep-interval-updates'):
        cmd_extra_args.extend(['--keep-interval-updates', '10'])
    if not argv_has(cmd_extra_args, '--keep-last-epochs'):
        cmd_extra_args.extend(['--keep-last-epochs', '10'])

    return cmd_extra_args


def verify_tensorboard_dependencies(parser):
    try:
        import tensorflow
        import tensorboard
    except ImportError:
        raise CLIArgsException(parser, '"--tensorboard-port" options requires "tensorflow" and "tensorboard" '
                                       'python modules, but they could not be found, please install them using pip3')


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description='Train the neural model', prog='mmt train')
    parser.add_argument('data_path', metavar='DATA_FOLDER',
                        help='data folder holding binarized training and validation sets')
    parser.add_argument('output_path', metavar='OUTPUT', help='the model output path')
    parser.add_argument('-n', '--checkpoints-num', dest='num_checkpoints', type=int, default=10,
                        help='number of checkpoints to average (default is 10)')
    parser.add_argument('-w', '--working-dir', metavar='WORKING_DIR', dest='wdir', default=None,
                        help='the working directory for temporary files (default is os temp folder)')
    parser.add_argument('-d', '--debug', action='store_true', dest='debug', default=False,
                        help='prevents temporary files to be removed after execution')
    parser.add_argument('--log', dest='log_file', default=None, help='detailed log file')
    parser.add_argument('--resume', action='store_true', dest='resume', default=False,
                        help='resume training from last saved checkpoint even after training completion')
    parser.add_argument('--from-model', dest='init_model', default=None,
                        help='start the training from the specified model.pt file')
    parser.add_argument('--gpus', dest='gpus', nargs='+', type=int, default=None,
                        help='the list of GPUs available for training (default is all available GPUs)')
    parser.add_argument('--tensorboard-port', dest='tensorboard_port', type=int, default=None,
                        help='if specified, starts a tensorboard instance during training on the given port')

    args, extra_argv = parser.parse_known_args(argv)
    if args.debug and args.wdir is None:
        raise CLIArgsException(parser, '"--debug" options requires explicit working dir with "--working-dir"')

    if args.tensorboard_port is not None:
        verify_tensorboard_dependencies(parser)

    return args, parse_extra_argv(parser, extra_argv)


def main(argv=None):
    args, extra_argv = parse_args(argv)
    activity = TrainActivity(args, extra_argv, wdir=args.wdir, log_file=args.log_file, delete_on_exit=not args.debug)
    activity.run()
