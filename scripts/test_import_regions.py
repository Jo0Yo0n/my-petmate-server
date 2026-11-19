"""Run with: python3 -m unittest discover -s scripts -p 'test_*.py'."""
import hashlib
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import urllib.error

import import_regions as importer


def row(code, name, parent, full_name=None):
    return {'region_cd': code, 'sido_cd': code[:2], 'sgg_cd': code[2:5],
            'umd_cd': code[5:8], 'ri_cd': code[8:], 'locallow_nm': name,
            'locatadd_nm': full_name or name, 'locathigh_cd': parent}


def sample_rows():
    return {r['region_cd']: r for r in [
        row('1100000000', '서울특별시', '0000000000'),
        row('1111000000', '종로구', '1100000000', '서울특별시 종로구'),
        row('1111010100', '청운동', '1111000000', '서울특별시 종로구 청운동'),
        row('4100000000', '경기도', '0000000000'),
        row('4127000000', '안산시', '4100000000', '경기도 안산시'),
        row('4127100000', '상록구', '4127000000', '경기도 안산시 상록구'),
        row('4127110100', '일동', '4127100000', '경기도 안산시 상록구 일동'),
        row('4182000000', '가평군', '4100000000', '경기도 가평군'),
        row('4182025000', '가평읍', '4182000000', '경기도 가평군 가평읍'),
        row('4182025021', '읍내리', '4182025000', '경기도 가평군 가평읍 읍내리'),
    ]}


class ImportRegionsTest(unittest.TestCase):
    def test_flattens_ansan_preserves_town_and_excludes_ri(self):
        regions = importer.convert(sample_rows())
        self.assertEqual(regions['4127100000'], ('안산시 상록구', 'sgg', '4100000000'))
        self.assertEqual(regions['4182025000'], ('가평읍', 'umd', '4182000000'))
        self.assertNotIn('4127000000', regions)
        self.assertNotIn('4182025021', regions)

    def test_missing_parent_stops_conversion(self):
        rows = sample_rows()
        del rows['4127100000']
        with self.assertRaises(ValueError):
            importer.convert(rows)

    def test_same_name_in_different_districts_has_distinct_ids(self):
        self.assertNotEqual(importer.region_id('1111010100'), importer.region_id('4127110100'))
        self.assertEqual(importer.region_id('4127110100'), importer.region_id('4127110100'))

    def test_sql_is_deterministic_escaped_and_transactional(self):
        regions = importer.convert(sample_rows())
        regions['4127110100'] = ("작은'동", 'umd', '4127100000')
        sql = importer.sql_text(regions)
        self.assertIn("작은''동", sql)
        self.assertIn('ON CONFLICT (id) DO UPDATE', sql)
        self.assertIn('BEGIN;', sql)
        self.assertTrue(sql.endswith('COMMIT;\n'))
        self.assertEqual(sql, importer.sql_text(dict(reversed(list(regions.items())))))

    def test_api_failure_is_not_treated_as_empty_success(self):
        with self.assertRaises(ValueError):
            importer.parse_page(b'<OpenAPI_ServiceResponse><error>denied</error></OpenAPI_ServiceResponse>')

    def test_request_exception_does_not_leak_key(self):
        with tempfile.TemporaryDirectory() as tmp, patch.object(
            importer.urllib.request, 'urlopen', side_effect=urllib.error.URLError('SECRET')
        ):
            with self.assertRaises(ValueError) as caught:
                importer.download(Path(tmp), 'SECRET')
            self.assertNotIn('SECRET', str(caught.exception))

    def test_encoded_and_decoded_keys_produce_the_same_request(self):
        urls = []
        def fail(url, **kwargs):
            urls.append(url)
            raise urllib.error.URLError('failure')
        with tempfile.TemporaryDirectory() as tmp, patch.object(importer.urllib.request, 'urlopen', fail):
            for key in ('a+b/c==', 'a%2Bb%2Fc%3D%3D'):
                with self.assertRaises(ValueError):
                    importer.download(Path(tmp), key)
        self.assertEqual(urls[0], urls[1])
        self.assertIn('a%2Bb%2Fc%3D%3D', urls[0])

    def test_saved_collection_rejects_corruption_and_incomplete_pagination(self):
        # Exercise the real saved API response shape without network access.
        with tempfile.TemporaryDirectory() as tmp:
            directory = Path(tmp)
            pages = []
            for scope in importer.SCOPES:
                code = scope + '00000000'
                fields = ''.join(f'<{k}>{v}</{k}>' for k, v in sample_rows()[code].items())
                data = (f'<StanReginCd><head><totalCount>1</totalCount><pageNo>1</pageNo>'
                        f'<RESULT><resultCode>INFO-0</resultCode></RESULT></head><row>{fields}</row>'
                        '</StanReginCd>').encode()
                filename = f'{scope}-0001.xml'
                (directory / filename).write_bytes(data)
                pages.append({'file': filename, 'sha256': hashlib.sha256(data).hexdigest()})
            manifest = {'scopes': importer.SCOPES, 'pages': pages}
            path = directory / 'manifest.json'
            path.write_text(json.dumps(manifest))
            self.assertEqual(len(importer.load_rows(directory)), 2)
            original = (directory / '11-0001.xml').read_bytes()
            (directory / '11-0001.xml').write_bytes(original + b' ')
            with self.assertRaisesRegex(ValueError, 'checksum'):
                importer.load_rows(directory)
            changed = original.replace(b'<totalCount>1', b'<totalCount>2')
            (directory / '11-0001.xml').write_bytes(changed)
            manifest['pages'][0]['sha256'] = hashlib.sha256(changed).hexdigest()
            path.write_text(json.dumps(manifest))
            with self.assertRaisesRegex(ValueError, 'Incomplete'):
                importer.load_rows(directory)


if __name__ == '__main__':
    unittest.main()
