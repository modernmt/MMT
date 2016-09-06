import os

__author__ = 'Davide Caroselli'

__self_dir = os.path.dirname(os.path.realpath(__file__))

MMT_VERSION = '0.13'

PYOPT_DIR = os.path.join(__self_dir, 'opt')
MMT_ROOT = os.path.abspath(os.path.join(__self_dir, os.pardir))
ENGINES_DIR = os.path.join(MMT_ROOT, 'engines')
RUNTIME_DIR = os.path.join(MMT_ROOT, 'runtime')
BUILD_DIR = os.path.join(MMT_ROOT, 'build')

LIB_DIR = os.path.join(BUILD_DIR, 'lib')
BIN_DIR = os.path.join(BUILD_DIR, 'bin')

MMT_JAR = os.path.join(BUILD_DIR, 'mmt-' + MMT_VERSION + '.jar')

# Environment setup
os.environ['LD_LIBRARY_PATH'] = LIB_DIR
os.environ['LC_ALL'] = 'en_US.UTF-8'
os.environ['LANG'] = 'en_US.UTF-8'

if not 'MMT_HAZELCAST_PUBLIC_ADDRESS' in os.environ:
    import subprocess
    ip = subprocess.check_output(["hostname","-I"]).strip().split()[0]
    os.environ['MMT_HAZELCAST_PUBLIC_ADDRESS'] = ip

def mmt_javamain(main_class, args=None, hserr_path=None, remote_debug=False):
    command = ['java', '-cp', MMT_JAR,
               '-Dmmt.home=' + MMT_ROOT,
               '-Djava.library.path=' + LIB_DIR,
               '-Dhazelcast.public_address=' + os.environ['MMT_HAZELCAST_PUBLIC_ADDRESS'],
               main_class]
    
    if remote_debug:
        command.insert(1, '-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005')

    if hserr_path is not None:
        command.insert(1, '-XX:ErrorFile=' + os.path.join(hserr_path, 'hs_err_pid%p.log'))

    if args is not None:
        command += args

    return command


class IllegalStateException(Exception):
    def __init__(self, error):
        self.message = error


class IllegalArgumentException(Exception):
    def __init__(self, error):
        self.message = error
