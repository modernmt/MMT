# coding=utf-8
import codecs
import os
import re

import sys

__author__ = 'Davide Caroselli'


def _abspath(root, path):
    if not os.path.isabs(path):
        path = os.path.abspath(os.path.join(root, path))
    return path


def _class(classname, super_class=None):
    lines = ['%public',
             '%class ' + classname]

    if super_class is not None:
        lines.append('%extends ' + super_class)

    lines += ['%unicode',
              '%integer',
              '%function next',
              '%pack',
              '%char',
              '%{',
              '\tprotected int getStartRead() { return zzStartRead; }',
              '\tprotected int getMarkedPosition() { return zzMarkedPos; }',
              '\tprotected int yychar() { return yychar; }',
              '%}']

    return '\n'.join(lines)


def _include(path):
    with codecs.open(path, encoding='utf-8') as content:
        return content.read()


def _process_prefix(line, caseless, patterns):
    # Match any case only if caseless has been specified and line is not a single char
    match_anycase = False

    if caseless and len(line) > 1 and line[0].isalpha():
        line = line.lower()
        match_anycase = True

    # No duplicates
    if line in patterns:
        return None

    if match_anycase:
        string = ''
        for i in range(0, len(line)):
            c = line[i]
            string += '(' + re.escape(c.upper()) + '|' + re.escape(c.lower()) + ')'
        line = string + '\\.'
    else:
        line = re.escape(line + '.')

    return '(' + line + ')'


def _prefixes(path, caseless=None):
    if caseless is not None and caseless != 'caseless':
        raise Exception('Unrecognized argument ' + caseless)

    caseless = False if caseless is None else True

    regular_patterns = []
    numeric_only_patterns = []

    with codecs.open(path, encoding='utf-8') as source:
        for line in source:
            line = line.strip()

            if len(line) == 0 or line.startswith('#'):
                continue

            if '#NUMERIC_ONLY#' in line:
                patterns = numeric_only_patterns
                line = line.replace('#NUMERIC_ONLY#', '').strip()
            else:
                patterns = regular_patterns

            line = _process_prefix(line, caseless, patterns)

            if line is not None:
                patterns.append(line)

    return regular_patterns, numeric_only_patterns


def _contractions(path):
    result = []

    with codecs.open(path, encoding='utf-8') as source:
        for line in source:
            line = line.strip()

            if len(line) == 0 or line.startswith('#'):
                continue

            for token in (line.lower(), line.upper(), line[:1].upper() + line[1:].lower()):
                pattern = token.replace('\'', '{_}?{apos}{_}?')
                if pattern.endswith('{_}?'):
                    pattern = pattern[:-4]

                result.append(pattern)

    return result


def _encode_prefixes(regular_patterns, numeric_only_patterns):
    lines = []

    if len(regular_patterns) > 0:
        lines.append('ProtectedPatterns = (' + '|'.join(regular_patterns) + ')')
    if len(numeric_only_patterns) > 0:
        lines.append('NumericProtectedPatters = (' + '|'.join(numeric_only_patterns) + ')')

    return '\n'.join(lines)


def generate_jflex(parent_dir, template_file, target_dir):
    relpath = os.path.dirname(template_file)
    include_root = os.path.join(parent_dir, relpath)

    package = relpath.replace(os.path.sep, '.')
    classname = os.path.splitext(os.path.basename(template_file))[0]

    template_file = os.path.join(parent_dir, template_file)
    parent_target = os.path.join(target_dir, relpath)
    target_file = os.path.join(parent_target, classname + '.jflex')

    content = []

    with codecs.open(template_file, encoding='utf-8') as template:
        for l in template:
            content.append(l.strip())

    has_regular_patterns = False
    has_numeric_only_patterns = False

    for i in range(0, len(content)):
        l = content[i]

        if l.startswith('//pyflex '):
            l = l.split()[1:]
            args = l[1:]
            command = l[0]

            if command == 'class':
                content[i] = _class(classname, super_class=(args[0] if len(args) > 0 else None))
            elif command == 'include':
                content[i] = _include(_abspath(include_root, args[0]))
            elif command == 'prefixes':
                regular_patterns, numeric_only_patterns = _prefixes(_abspath(include_root, args[0]), *(args[1:]))
                has_regular_patterns = len(regular_patterns) > 0
                has_numeric_only_patterns = len(numeric_only_patterns) > 0

                content[i] = _encode_prefixes(regular_patterns, numeric_only_patterns)
            elif command == 'contractions':
                contractions = _contractions(_abspath(include_root, args[0]))
                content[i] = 'Contractions = (%s)' % ('|'.join(contractions))
            else:
                raise Exception("Unknown command " + command)

    if has_regular_patterns:
        content.append('[^[:letter:]]{ProtectedPatterns}[^[:letter:]] '
                       '{ zzStartReadOffset = 1; yypushback(1); return PROTECT; }')
    if has_numeric_only_patterns:
        content.append('[^[:letter:]]{NumericProtectedPatters}{_}[:digit:] '
                       '{ zzStartReadOffset = 1; yypushback(2); return PROTECT; }')

    if not os.path.isdir(parent_target):
        os.makedirs(parent_target)

    with open(target_file, 'wb') as output:
        output.write(('\n'.join(content)).encode('utf-8'))


def main():
    if len(sys.argv) != 3:
        print 'Usage: pyflex.py SOURCE_DIRECTORY TARGET_DIRECTORY'
        exit(1)

    source_dir = sys.argv[1]
    target_dir = sys.argv[2]

    source_files = []

    for root, directories, filenames in os.walk(source_dir):
        for filename in filenames:
            if filename.endswith('.pyflex'):
                relpath = root.replace(source_dir, '').lstrip(os.path.sep)
                source_files.append(os.path.join(relpath, filename))

    for f in source_files:
        generate_jflex(source_dir, f, target_dir)


if __name__ == "__main__":
    main()
