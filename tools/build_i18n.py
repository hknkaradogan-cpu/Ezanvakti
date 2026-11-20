#!/usr/bin/env python3
import json, os, sys
sys.path.insert(0, os.path.dirname(__file__))
from i18n_src import T
out = {k: {'en': v[0], 'ar': v[1]} for k, v in T.items()}
p = os.path.join(os.path.dirname(__file__), '..', 'web', 'data', 'i18n.json')
json.dump(out, open(p, 'w', encoding='utf-8'), ensure_ascii=False, separators=(',', ':'))
print('i18n', len(out))
