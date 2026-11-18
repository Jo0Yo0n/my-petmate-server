#!/usr/bin/env python3
"""Download Seoul/Gyeonggi XML and generate PostgreSQL seed SQL (stdlib only)."""
import argparse
from collections import Counter
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import sys
import tempfile
import urllib.error
import urllib.parse
import urllib.request
import uuid
import xml.etree.ElementTree as ET

ENDPOINT = 'https://apis.data.go.kr/1741000/StanReginCd/getStanReginCdList'
SCOPES = {'11': '서울특별시', '41': '경기도'}
# Public, fixed namespace: changing this changes every generated ID.
NAMESPACE = uuid.UUID('d0a018ba-8574-4ec6-bb87-1dbf90e378cc')


def parse_page(data):
    root = ET.fromstring(data)
    if root.findtext('head/RESULT/resultCode') != 'INFO-0':
        raise ValueError('API returned an unsuccessful result')
    total = int(root.findtext('head/totalCount', '-1'))
    page = int(root.findtext('head/pageNo', '-1'))
    rows = [{child.tag: child.text or '' for child in row} for row in root.findall('row')]
    if total <= 0 or page <= 0 or not rows:
        raise ValueError('Empty or invalid API page')
    return total, page, rows


def download(directory, key):
    pages = []
    for code, name in SCOPES.items():
        received, expected, page = 0, None, 1
        while expected is None or received < expected:
            query = urllib.parse.urlencode({
                'ServiceKey': urllib.parse.unquote(key), 'pageNo': page,
                'numOfRows': 1000, 'type': 'xml', 'locatadd_nm': name,
            })
            try:
                with urllib.request.urlopen(ENDPOINT + '?' + query, timeout=30) as response:
                    data = response.read()
            except (urllib.error.URLError, TimeoutError):
                # Exceptions may contain the authenticated URL. Never print them.
                raise ValueError(f'API request failed: scope={code}, page={page}') from None
            total, actual_page, rows = parse_page(data)
            if actual_page != page or (expected is not None and total != expected):
                raise ValueError('Page number or total changed during download; retry collection')
            expected = total
            received += len(rows)
            if received > expected:
                raise ValueError('Received more rows than totalCount')
            filename = f'{code}-{page:04d}.xml'
            (directory / filename).write_bytes(data)
            pages.append({'file': filename, 'sha256': hashlib.sha256(data).hexdigest()})
            page += 1
        print(f'{name}: {received} source rows, {page - 1} pages')
    (directory / 'manifest.json').write_text(json.dumps({
        'source': ENDPOINT, 'collected_at': datetime.now(timezone.utc).isoformat(),
        'scopes': SCOPES, 'pages': pages,
    }, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def load_rows(directory):
    manifest = json.loads((directory / 'manifest.json').read_text(encoding='utf-8'))
    if manifest['scopes'] != SCOPES:
        raise ValueError('Unexpected collection scope')
    rows, seen_files = {}, set()
    for scope in SCOPES:
        entries = [p for p in manifest['pages'] if p['file'].startswith(scope + '-')]
        received, expected = 0, None
        for page, entry in enumerate(entries, 1):
            filename = entry['file']
            if filename != f'{scope}-{page:04d}.xml' or filename in seen_files:
                raise ValueError('Missing, duplicate, or out-of-order page')
            seen_files.add(filename)
            data = (directory / filename).read_bytes()
            if hashlib.sha256(data).hexdigest() != entry['sha256']:
                raise ValueError('XML checksum mismatch')
            total, actual_page, items = parse_page(data)
            if actual_page != page or (expected is not None and expected != total):
                raise ValueError('Inconsistent pagination')
            expected = total
            received += len(items)
            for row in items:
                code = row.get('region_cd', '')
                if not re.fullmatch(r'\d{10}', code) or not code.startswith(scope):
                    raise ValueError('Invalid or out-of-scope region code')
                if code in rows:
                    raise ValueError('Duplicate region code')
                if code != ''.join(row.get(k, '') for k in ('sido_cd', 'sgg_cd', 'umd_cd', 'ri_cd')):
                    raise ValueError('Region code components do not match')
                rows[code] = row
        if expected is None or received != expected:
            raise ValueError('Incomplete collection')
    if len(seen_files) != len(manifest['pages']):
        raise ValueError('Unexpected manifest page')
    return rows


def convert(rows):
    selected = {}
    for scope in SCOPES:
        code = scope + '00000000'
        if code not in rows or rows[code]['locatadd_nm'] != SCOPES[scope]:
            raise ValueError('Missing province root')
        selected[code] = (SCOPES[scope], 'sido', None)
    for code, row in rows.items():
        if row['ri_cd'] != '00' or row['umd_cd'] == '000':
            continue
        parent_code = row.get('locathigh_cd')
        parent = rows.get(parent_code)
        if parent is None or parent['umd_cd'] != '000' or parent['sgg_cd'] == '000':
            raise ValueError('Missing or unsupported locality parent')
        root_code = code[:2] + '00000000'
        prefix = SCOPES[code[:2]] + ' '
        if not parent['locatadd_nm'].startswith(prefix):
            raise ValueError('District name is outside province')
        # Flatten city -> non-autonomous district into a single named sgg.
        selected[parent_code] = (parent['locatadd_nm'][len(prefix):], 'sgg', root_code)
        selected[code] = (row['locallow_nm'], 'umd', parent_code)
    if not any(v[1] == 'umd' for v in selected.values()):
        raise ValueError('No selectable localities')
    for name, level, parent in selected.values():
        if not name.strip() or len(name) > 100 or '\x00' in name:
            raise ValueError('Invalid region name')
    return selected


def region_id(code):
    return str(uuid.uuid5(NAMESPACE, code))


def sql_text(regions):
    def quote(value):
        return "'" + value.replace("'", "''") + "'"
    lines = ['-- Generated by scripts/import_regions.py; source codes map to fixed UUIDv5 IDs.',
             '-- Scope: Seoul and Gyeonggi. No deletion or abolition handling.',
             'BEGIN;', 'SET LOCAL standard_conforming_strings = on;',
             'LOCK TABLE region IN SHARE ROW EXCLUSIVE MODE;']
    for level in ('sido', 'sgg', 'umd'):
        values = []
        for code, (name, item_level, parent) in sorted(regions.items()):
            if item_level == level:
                parent_sql = quote(region_id(parent)) if parent else 'NULL'
                values.append(f'({quote(region_id(code))}, {quote(name)}, {quote(level)}, {parent_sql})')
        if not values:
            raise ValueError('Missing catalog level')
        lines += ['INSERT INTO region (id, name, level, parent_id) VALUES', ',\n'.join(values),
                  'ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name,',
                  '    level = EXCLUDED.level, parent_id = EXCLUDED.parent_id;']
    return '\n'.join(lines + ['COMMIT;', ''])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output-dir', type=Path, default=Path('data/regions'))
    parser.add_argument('--from-xml', type=Path, help='Convert a saved directory without an API key')
    args = parser.parse_args()
    if args.from_xml:
        directory = args.from_xml
        regions = convert(load_rows(directory))
        (directory / 'regions.sql').write_text(sql_text(regions), encoding='utf-8')
    else:
        key = os.environ.get('DATA_GO_KR_SERVICE_KEY', '').strip()
        if not key:
            raise ValueError('Set DATA_GO_KR_SERVICE_KEY in the environment')
        target = args.output_dir
        if target.exists():
            raise ValueError('Output directory already exists; use --from-xml or a new directory')
        target.parent.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(prefix='.regions-', dir=target.parent) as temp:
            directory = Path(temp)
            download(directory, key)
            regions = convert(load_rows(directory))
            (directory / 'regions.sql').write_text(sql_text(regions), encoding='utf-8')
            directory.rename(target)
        directory = target
    print('Catalog:', dict(Counter(v[1] for v in regions.values())))
    print(f'Saved XML, manifest and SQL: {directory}')


if __name__ == '__main__':
    try:
        main()
    except (ValueError, OSError, ET.ParseError, KeyError) as error:
        # Avoid accidentally displaying response content, paths or credentials.
        print(f'Import failed ({type(error).__name__}). Check input, API availability and configuration.', file=sys.stderr)
        sys.exit(1)
