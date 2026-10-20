"""Authentication fixtures for Schemathesis against a disposable contract database."""

from uuid import uuid4

import requests
import schemathesis


@schemathesis.auth(refresh_interval=None)
class ContractAuth:
    def __init__(self):
        self.tokens = {}

    def get(self, case, ctx):
        base_url = ctx.operation.base_url.rstrip("/")
        # Withdrawal must not invalidate the account used by other test cases.
        withdrawal = case.path == "/api/guardians/me/withdraw"
        if withdrawal or base_url not in self.tokens:
            response = requests.post(
                base_url + "/api/auth/signup",
                json={
                    "email": "contract-" + uuid4().hex + "@example.com",
                    "password": "ContractPass123!",
                    "profileType": "family",
                    "identityVisibility": "public",
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
