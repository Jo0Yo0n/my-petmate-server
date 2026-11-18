from types import SimpleNamespace
from unittest import TestCase
from unittest.mock import Mock, patch

from contract_hooks import ACTIVITY_REGION_IDS, ContractAuth, map_case
from schemathesis.generation.meta import TestPhase
from schemathesis.generation.modes import GenerationMode


class ContractAuthTest(TestCase):
    @patch("contract_hooks.requests.post")
    def test_withdrawal_account_does_not_replace_shared_account(self, post):
        post.side_effect = [
            Mock(status_code=201, json=lambda: {"accessToken": "shared"}),
            Mock(status_code=201, json=lambda: {"accessToken": "withdrawal-1"}),
            Mock(status_code=201, json=lambda: {"accessToken": "withdrawal-2"}),
        ]
        provider = ContractAuth()
        ctx = SimpleNamespace(operation=SimpleNamespace(base_url="http://localhost:8080/"))
        me = SimpleNamespace(path="/api/guardians/me", headers={"X-Test": "preserve"})
        withdrawal = SimpleNamespace(path="/api/guardians/me/withdraw")

        self.assertEqual(provider.get(me, ctx), "shared")
        self.assertEqual(provider.get(withdrawal, ctx), "withdrawal-1")
        self.assertEqual(provider.get(withdrawal, ctx), "withdrawal-2")
        self.assertEqual(provider.get(me, ctx), "shared")
        self.assertEqual(post.call_count, 3)
        emails = [call.kwargs["json"]["email"] for call in post.call_args_list]
        self.assertEqual(len(set(emails)), 3)
        provider.set(me, "shared", ctx)
        self.assertEqual(me.headers["Authorization"], "Bearer shared")
        self.assertEqual(me.headers["X-Test"], "preserve")

    @patch("contract_hooks.requests.post")
    def test_setup_failure_does_not_expose_response_credentials(self, post):
        post.return_value = Mock(status_code=500, text="secret-token")
        ctx = SimpleNamespace(operation=SimpleNamespace(base_url="http://localhost:8080"))
        with self.assertRaisesRegex(RuntimeError, "HTTP 500") as error:
            ContractAuth().get(SimpleNamespace(path="/api/guardians/me"), ctx)
        self.assertNotIn("secret-token", str(error.exception))


class ActivityRegionFixtureTest(TestCase):
    def test_replaces_generated_positive_activity_ids_for_signup_and_update(self):
        for method, path in (("POST", "/api/auth/signup"), ("PATCH", "/api/guardians/me")):
            with self.subTest(method=method, path=path):
                case = self.make_case(method, path, GenerationMode.POSITIVE, ["unknown-1", "unknown-2"])
                self.assertIs(map_case(None, case), case)
                self.assertEqual(case.body["activityRegionIds"], list(ACTIVITY_REGION_IDS[:2]))
                self.assertEqual(case.body["nickname"], "guardian")

    def test_preserves_negative_cases_and_omitted_or_empty_lists(self):
        for mode, region_ids in (
            (GenerationMode.NEGATIVE, ["unknown"]),
            (GenerationMode.POSITIVE, None),
            (GenerationMode.POSITIVE, []),
        ):
            with self.subTest(mode=mode, region_ids=region_ids):
                case = self.make_case("POST", "/api/auth/signup", mode, region_ids)
                before = case.body.copy()
                self.assertEqual(map_case(None, case).body, before)

    @staticmethod
    def make_case(method, path, mode, region_ids):
        body = {"nickname": "guardian"}
        if region_ids is not None:
            body["activityRegionIds"] = region_ids
        return SimpleNamespace(
            method=method, path=path, body=body,
            meta=SimpleNamespace(
                generation=SimpleNamespace(mode=mode), phase=SimpleNamespace(name=TestPhase.FUZZING),
            ),
        )


class ActivityTimeFixtureTest(TestCase):
    def test_repairs_reversed_equal_duplicate_and_overlapping_ranges(self):
        first = self.slot("09:00", "10:00")
        for slots in (
            [self.slot("10:00", "09:00")],
            [self.slot("23:50", "23:50")],
            [first, first],
            [self.slot("09:50", "10:10"), first],
        ):
            with self.subTest(slots=slots):
                case = self.make_case(slots)
                normalized = map_case(None, case).body["availableTimeSlots"]
                self.assertEqual(
                    normalized,
                    [self.slot("09:00", "09:10"), self.slot("09:10", "09:20")][:len(slots)],
                )

    def test_preserves_valid_slots_and_leaves_negative_or_explicit_cases_unchanged(self):
        valid = [self.slot("23:40", "23:50"), self.slot("23:50", "24:00")]
        invalid = [self.slot("10:00", "09:00")]
        for case in (
            self.make_case(valid),
            self.make_case([self.slot("09:05", "09:00")]),
            self.make_case(invalid, mode=GenerationMode.NEGATIVE),
            self.make_case(invalid, phase=TestPhase.EXAMPLES),
        ):
            with self.subTest(body=case.body, mode=case.meta.generation.mode):
                before = case.body
                self.assertIs(map_case(None, case).body, before)

    @staticmethod
    def slot(start, end):
        return {"dayOfWeek": "mon", "startTime": start, "endTime": end}

    @staticmethod
    def make_case(slots, mode=GenerationMode.POSITIVE, phase=TestPhase.COVERAGE):
        return SimpleNamespace(
            method="PATCH",
            path="/api/guardians/me",
            body={"availableTimeSlots": slots},
            meta=SimpleNamespace(
                generation=SimpleNamespace(mode=mode), phase=SimpleNamespace(name=phase),
            ),
        )
