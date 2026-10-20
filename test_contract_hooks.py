from types import SimpleNamespace
from unittest import TestCase
from unittest.mock import Mock, patch

from contract_hooks import ContractAuth


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
