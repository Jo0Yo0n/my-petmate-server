"""Fixtures for Schemathesis against a disposable contract database."""

import re
from uuid import uuid4

import requests
import schemathesis
from schemathesis.generation.meta import TestPhase


# Locality IDs loaded by scripts/fixtures/ansan-regions.sql.
ACTIVITY_REGION_IDS = (
    "c6cd82c5-907c-50f8-b341-44c379916e78",
    "e17a6e67-2cc5-58e9-be85-fd029ea4f53a",
    "c6a667c5-fdf8-5558-a3f8-0eb7ea59e4db",
)
ACTIVITY_OPERATIONS = {
    ("POST", "/api/auth/signup"),
    ("PATCH", "/api/guardians/me"),
}


SLOT_TIME = re.compile(r"(?:(?:[01][0-9]|2[0-3]):[0-5]0|24:00)")
WEEKDAYS = ("mon", "tue", "wed", "thu", "fri", "sat", "sun")


def has_conflicting_time_slots(slots):
    ranges = []
    for slot in slots:
        if not isinstance(slot, dict) or slot.keys() != {"dayOfWeek", "startTime", "endTime"}:
            return False
        day, start, end = slot["dayOfWeek"], slot["startTime"], slot["endTime"]
        if (
            day not in WEEKDAYS
            or not isinstance(start, str)
            or not isinstance(end, str)
            or not SLOT_TIME.fullmatch(start)
            or start == "24:00"
            or not SLOT_TIME.fullmatch(end)
        ):
            return False
        ranges.append((day, start, end))

    previous_ends = {}
    for day, start, end in sorted(ranges):
        # Valid HH:mm values sort chronologically, including the end-of-day boundary.
        if start >= end or start < previous_ends.get(day, "00:00"):
            return True
        previous_ends[day] = end
    return False


def ten_minute_slot(index):
    start = 9 * 60 + index * 10
    end = start + 10
    return {
        "dayOfWeek": "mon",
        "startTime": f"{start // 60:02d}:{start % 60:02d}",
        "endTime": f"{end // 60:02d}:{end % 60:02d}",
    }


@schemathesis.hook
def map_case(context, case):
    if (
        (case.method.upper(), case.path) in ACTIVITY_OPERATIONS
        and case.meta is not None
        and case.meta.generation.mode.is_positive
        and isinstance(case.body, dict)
    ):
        region_ids = case.body.get("activityRegionIds")
        if isinstance(region_ids, list) and 1 <= len(region_ids) <= len(ACTIVITY_REGION_IDS):
            case.body = {**case.body, "activityRegionIds": list(ACTIVITY_REGION_IDS[:len(region_ids)])}

        slots = case.body.get("availableTimeSlots")
        if (
            case.meta.phase.name != TestPhase.EXAMPLES
            and isinstance(slots, list)
            and 1 <= len(slots) <= 21
            and has_conflicting_time_slots(slots)
        ):
            case.body = {
                **case.body,
                "availableTimeSlots": [ten_minute_slot(index) for index in range(len(slots))],
            }
    return case


@schemathesis.auth(refresh_interval=None)
class ContractAuth:
    def __init__(self):
        self.tokens = {}

    def get(self, case, ctx):
        base_url = ctx.operation.base_url.rstrip("/")
        # Withdrawal must not invalidate the account used by other test cases.
        withdrawal = case.path == "/api/guardians/me/withdraw"
        if withdrawal or base_url not in self.tokens:
            account_id = uuid4().hex
            response = requests.post(
                base_url + "/api/auth/signup",
                json={
                    "email": "contract-" + account_id + "@example.com",
                    "password": "ContractPass123!",
                    "profileType": "family",
                    "discoveryStatus": "paused",
                    "nickname": account_id[:20],
                },
                timeout=10,
            )
            if response.status_code != 201:
                # Do not include the response body, which may contain credentials.
                raise RuntimeError(
                    "Contract authentication signup failed: HTTP "
                    + str(response.status_code)
                )
            token = response.json()["accessToken"]
            if withdrawal:
                return token
            self.tokens[base_url] = token
        return self.tokens[base_url]

    def set(self, case, data, ctx):
        if case.headers is None:
            case.headers = {}
        case.headers["Authorization"] = "Bearer " + data
