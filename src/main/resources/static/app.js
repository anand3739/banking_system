(function () {
    "use strict";

    const state = {
        apiBaseUrl: localStorage.getItem("banking.apiBaseUrl") || window.location.origin
    };

    const els = {
        apiConfigForm: document.getElementById("api-config-form"),
        apiBaseUrl: document.getElementById("apiBaseUrl"),
        output: document.getElementById("api-output"),
        toast: document.getElementById("toast"),
        clearOutputBtn: document.getElementById("clear-output-btn"),

        createCustomerForm: document.getElementById("create-customer-form"),
        createAccountForm: document.getElementById("create-account-form"),
        accountDetailsForm: document.getElementById("account-details-form"),
        accountBalanceForm: document.getElementById("account-balance-form"),
        depositForm: document.getElementById("deposit-form"),
        withdrawForm: document.getElementById("withdraw-form"),
        transferForm: document.getElementById("transfer-form"),
        historyForm: document.getElementById("history-form"),
        loadAlertsBtn: document.getElementById("load-alerts-btn"),

        historyTableBody: document.querySelector("#history-table tbody"),
        alertsTableBody: document.querySelector("#alerts-table tbody")
    };

    let toastTimer = null;

    init();

    function init() {
        els.apiBaseUrl.value = state.apiBaseUrl;
        bindEvents();
        loadFraudAlerts();
    }

    function bindEvents() {
        els.apiConfigForm.addEventListener("submit", onApplyApiBaseUrl);
        els.clearOutputBtn.addEventListener("click", () => updateOutput("Ready."));

        els.createCustomerForm.addEventListener("submit", onCreateCustomer);
        els.createAccountForm.addEventListener("submit", onCreateAccount);
        els.accountDetailsForm.addEventListener("submit", onGetAccountDetails);
        els.accountBalanceForm.addEventListener("submit", onGetAccountBalance);
        els.depositForm.addEventListener("submit", onDeposit);
        els.withdrawForm.addEventListener("submit", onWithdraw);
        els.transferForm.addEventListener("submit", onTransfer);
        els.historyForm.addEventListener("submit", onLoadHistory);
        els.loadAlertsBtn.addEventListener("click", loadFraudAlerts);
    }

    async function onCreateCustomer(event) {
        event.preventDefault();
        const body = {
            name: document.getElementById("customerName").value.trim(),
            email: document.getElementById("customerEmail").value.trim(),
            phone: document.getElementById("customerPhone").value.trim()
        };
        await runAction("Customer created", () => request("/customers", "POST", body));
    }

    async function onCreateAccount(event) {
        event.preventDefault();
        const body = {
            customerId: toNumber(document.getElementById("accountCustomerId").value),
            accountType: document.getElementById("accountType").value,
            initialBalance: toAmount(document.getElementById("initialBalance").value)
        };
        await runAction("Account created", () => request("/accounts", "POST", body));
    }

    async function onGetAccountDetails(event) {
        event.preventDefault();
        const accountId = toNumber(document.getElementById("accountIdForDetails").value);
        await runAction("Account loaded", () => request(`/accounts/${accountId}`));
    }

    async function onGetAccountBalance(event) {
        event.preventDefault();
        const accountId = toNumber(document.getElementById("accountIdForBalance").value);
        await runAction("Balance loaded", () => request(`/accounts/${accountId}/balance`));
    }

    async function onDeposit(event) {
        event.preventDefault();
        const body = {
            accountId: toNumber(document.getElementById("depositAccountId").value),
            amount: toAmount(document.getElementById("depositAmount").value)
        };
        await runAction("Deposit successful", async () => {
            const response = await request("/transactions/deposit", "POST", body);
            await loadFraudAlerts();
            return response;
        });
    }

    async function onWithdraw(event) {
        event.preventDefault();
        const body = {
            accountId: toNumber(document.getElementById("withdrawAccountId").value),
            amount: toAmount(document.getElementById("withdrawAmount").value)
        };
        await runAction("Withdrawal successful", async () => {
            const response = await request("/transactions/withdraw", "POST", body);
            await loadFraudAlerts();
            return response;
        });
    }

    async function onTransfer(event) {
        event.preventDefault();
        const body = {
            senderAccountId: toNumber(document.getElementById("senderAccountId").value),
            receiverAccountId: toNumber(document.getElementById("receiverAccountId").value),
            amount: toAmount(document.getElementById("transferAmount").value)
        };
        await runAction("Transfer successful", async () => {
            const response = await request("/transactions/transfer", "POST", body);
            await loadFraudAlerts();
            return response;
        });
    }

    async function onLoadHistory(event) {
        event.preventDefault();
        const accountId = toNumber(document.getElementById("historyAccountId").value);
        await runAction("History loaded", async () => {
            const rows = await request(`/transactions/history/${accountId}`);
            renderHistory(rows);
            return rows;
        });
    }

    async function loadFraudAlerts() {
        await runAction("Fraud alerts loaded", async () => {
            const rows = await request("/fraud-alerts");
            renderAlerts(rows);
            return rows;
        }, false);
    }

    async function runAction(successMessage, fn, showSuccessToast = true) {
        try {
            const data = await fn();
            updateOutput(data);
            if (showSuccessToast) {
                toast(successMessage, "success");
            }
            return data;
        } catch (error) {
            updateOutput({ error: error.message });
            toast(error.message, "error");
            throw error;
        }
    }

    async function request(path, method = "GET", body = null) {
        const url = toAbsolute(path);
        const options = {
            method,
            headers: {
                "Accept": "application/json"
            }
        };

        if (body !== null) {
            options.headers["Content-Type"] = "application/json";
            options.body = JSON.stringify(body);
        }

        const response = await fetch(url, options);
        const payload = await parsePayload(response);

        if (!response.ok) {
            throw new Error(extractErrorMessage(payload, response.status, response.statusText));
        }

        return payload;
    }

    async function parsePayload(response) {
        const contentType = response.headers.get("content-type") || "";
        if (contentType.includes("application/json")) {
            return response.json();
        }
        const text = await response.text();
        return text || {};
    }

    function extractErrorMessage(payload, status, statusText) {
        if (payload && typeof payload === "object") {
            if (payload.validationErrors && Array.isArray(payload.validationErrors) && payload.validationErrors.length) {
                return payload.validationErrors.join("; ");
            }
            if (payload.message) {
                return payload.message;
            }
        }
        if (typeof payload === "string" && payload.trim()) {
            return payload;
        }
        return `${status} ${statusText}`;
    }

    function renderHistory(rows) {
        const data = Array.isArray(rows) ? rows : [];
        els.historyTableBody.innerHTML = "";
        if (!data.length) {
            els.historyTableBody.innerHTML = rowHtml(7, "No transactions found");
            return;
        }
        data.forEach((item) => {
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td>${safe(item.transactionId)}</td>
                <td>${safe(item.transactionType)}</td>
                <td>${safe(item.senderAccountId)}</td>
                <td>${safe(item.receiverAccountId)}</td>
                <td>${safe(item.amount)}</td>
                <td>${safe(item.status)}</td>
                <td>${safe(formatDate(item.transactionTime))}</td>
            `;
            els.historyTableBody.appendChild(tr);
        });
    }

    function renderAlerts(rows) {
        const data = Array.isArray(rows) ? rows : [];
        els.alertsTableBody.innerHTML = "";
        if (!data.length) {
            els.alertsTableBody.innerHTML = rowHtml(5, "No fraud alerts");
            return;
        }
        data.forEach((item) => {
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td>${safe(item.alertId)}</td>
                <td>${safe(item.transactionId)}</td>
                <td>${safe(item.fraudReason)}</td>
                <td>${safe(item.riskScore)}</td>
                <td>${safe(formatDate(item.flaggedAt))}</td>
            `;
            els.alertsTableBody.appendChild(tr);
        });
    }

    function rowHtml(colspan, text) {
        return `<tr><td colspan="${colspan}">${safe(text)}</td></tr>`;
    }

    function updateOutput(payload) {
        if (typeof payload === "string") {
            els.output.textContent = payload;
            return;
        }
        els.output.textContent = JSON.stringify(payload, null, 2);
    }

    function toAbsolute(path) {
        const base = normalizeBase(state.apiBaseUrl);
        const suffix = path.startsWith("/") ? path : `/${path}`;
        return `${base}${suffix}`;
    }

    function normalizeBase(input) {
        return (input || window.location.origin).trim().replace(/\/+$/, "");
    }

    function toNumber(value) {
        const n = Number(value);
        if (!Number.isFinite(n) || n <= 0) {
            throw new Error("Please enter a valid positive number.");
        }
        return n;
    }

    function toAmount(value) {
        const n = Number(value);
        if (!Number.isFinite(n) || n <= 0) {
            throw new Error("Please enter a valid amount greater than 0.");
        }
        return Number(n.toFixed(2));
    }

    function onApplyApiBaseUrl(event) {
        event.preventDefault();
        state.apiBaseUrl = normalizeBase(els.apiBaseUrl.value);
        localStorage.setItem("banking.apiBaseUrl", state.apiBaseUrl);
        toast(`API URL updated: ${state.apiBaseUrl}`, "success");
    }

    function toast(message, type) {
        if (toastTimer) {
            clearTimeout(toastTimer);
        }
        els.toast.textContent = message;
        els.toast.classList.remove("hidden", "success", "error");
        els.toast.classList.add(type === "error" ? "error" : "success");
        toastTimer = setTimeout(() => {
            els.toast.classList.add("hidden");
        }, 2800);
    }

    function formatDate(value) {
        if (!value) {
            return "";
        }
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return value;
        }
        return date.toLocaleString();
    }

    function safe(value) {
        if (value === null || value === undefined || value === "") {
            return "-";
        }
        return String(value)
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;");
    }
})();
