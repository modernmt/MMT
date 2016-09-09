import os
from multiprocessing import cpu_count

import cli
from moses import MosesFeature
from cli.libs import fileutils, shell

__author__ = 'Davide Caroselli'


class LanguageModel(MosesFeature):
    available_types = ['MultiplexedLM', 'KenLM']

    injector_section = 'lm'
    injectable_fields = {
        'order': ('LM order (N-grams length)', int, 5),
    }

    @staticmethod
    def instantiate(type_name, *model):
        if type_name == 'KenLM':
            return KenLM(*model)
        elif type_name == 'MultiplexedLM':
            return MultiplexedLM(*model)
        else:
            raise NameError('Invalid Language Model type: ' + type_name)

    def __init__(self, model, classname):
        MosesFeature.__init__(self, classname)

        self._order = None  # Injected
        self._model = model

    def train(self, corpora, lang, working_dir='.', log_file=None):
        if os.path.isfile(self._model):
            raise Exception('Model already exists at ' + self._model)

        parent_dir = os.path.abspath(os.path.join(self._model, os.pardir))
        if not os.path.isdir(parent_dir):
            fileutils.makedirs(parent_dir, exist_ok=True)

        if not os.path.isdir(working_dir):
            fileutils.makedirs(working_dir, exist_ok=True)


class KenLM(LanguageModel):
    def __init__(self, model):
        LanguageModel.__init__(self, model, 'KENLM')

        self.prune = True
        self._lmplz_bin = os.path.join(cli.BIN_DIR, 'kenlm', 'lmplz')
        self._bbinary_bin = os.path.join(cli.BIN_DIR, 'kenlm', 'build_binary')

    def train(self, corpora, lang, working_dir='.', log_file=None):
        LanguageModel.train(self, corpora, lang, working_dir, log_file)

        log = shell.DEVNULL

        try:
            if log_file is not None:
                log = open(log_file, 'w') if isinstance(log_file, str) else log_file

            # Collapse all corpora into a single text file
            merged_corpus = os.path.join(working_dir, 'merge')
            fileutils.merge([corpus.get_file(lang) for corpus in corpora], merged_corpus)

            # Create language model in ARPA format
            arpa_file = os.path.join(working_dir, 'lm.arpa')
            arpa_command = [self._lmplz_bin, '--discount_fallback', '-o', str(self._order),
                            '-S', str(self.get_mem_percent()) + '%', '-T', working_dir]
            if self._order > 2 and self.prune:
                arpa_command += ['--prune', '0', '0', '1']

            with open(merged_corpus) as stdin:
                with open(arpa_file, 'w') as stdout:
                    shell.execute(arpa_command, stdin=stdin, stdout=stdout, stderr=log)

            # Binarize ARPA file
            binarize_command = [self._bbinary_bin, arpa_file, self._model]
            shell.execute(binarize_command, stdout=log, stderr=log)
        finally:
            if log_file is not None and isinstance(log_file, str):
                log.close()

    @staticmethod
    def get_mem_percent():
        """:returns percentage of MemTotal (hardware memory) to use in `lmplz`."""
        # Simple heuristic: use 80% of *available* memory (instead of MemTotal as is the lmplz default) - avoids
        # crashing on machines with other jobs running.
        # This may evict some disk caches (is not too nice to other programs using mmapped
        # files unless you lower the 80%).

        mi = fileutils.meminfo()
        total = float(mi['MemTotal'])
        available = fileutils.free()
        return int(80.0 * available / total)

    def get_iniline(self):
        return 'factor=0 order={order} path={model}'.format(order=self._order, model=self.get_relpath(self._model))


class MultiplexedLM(LanguageModel):
    injector_section = 'lm'
    injectable_fields = {
        'alpha': ('Adaptive LM weight fraction from [0,1), the rest is assigned to background LM', float, 0.5),
        'function': ('interpolation function',
                     (basestring, ['interpolate-linear', 'interpolate-log-linear', 'interpolate-max']),
                     'interpolate-linear'),
    }

    def __init__(self, model):
        LanguageModel.__init__(self, model, 'MUXLM')

        self._function = None  # Injected
        self._alpha = None  # Injected

    def __get_config_line(self, name, model):
        return 'KENLM name={name} factor=0 order={order} path={model}'.format(name=name, model='${LM_PATH}' + model,
                                                                              order=self._order)

    def train(self, corpora, lang, working_dir='.', log_file=None):
        LanguageModel.train(self, corpora, lang, working_dir, log_file)

        bicorpora = []
        for corpus in corpora:
            if len(corpus.langs) > 1:
                bicorpora.append(corpus)

        log = shell.DEVNULL

        try:
            if log_file is not None:
                log = open(log_file, 'w')

            model_folder = os.path.dirname(self._model)
            config_content = ['[verbose]', '0', '[muxlm]']

            # Train static LM
            static_lm_model = os.path.join(model_folder, 'background.slm')
            static_lm = KenLM(static_lm_model)
            static_lm._order = self._order
            static_lm.train(corpora, lang, os.path.join(working_dir, 'background.slm'), log_file=log)
            config_content.append(self.__get_config_line('__background_lm__', 'background.slm'))

            # Train domain-specific LMs
            for corpus in bicorpora:
                adaptive_lm_model = os.path.join(model_folder, corpus.name + '.alm')
                adaptive_lm = KenLM(adaptive_lm_model)
                adaptive_lm.prune = False
                adaptive_lm._order = self._order
                adaptive_lm.train([corpus], lang, os.path.join(working_dir, corpus.name + '.alm'), log_file=log)
                config_content.append(self.__get_config_line(corpus.name, corpus.name + '.alm'))

            with open(self._model, 'w') as model:
                model.write('\n'.join(config_content))
                model.write('\n')
        finally:
            if log_file is not None:
                log.close()

    def get_iniline(self):
        return 'factor=0 path={model} background-lm=__background_lm__ alpha={alpha} function={function}'.format(
            model=self.get_relpath(self._model), alpha=self._alpha, function=self._function)
