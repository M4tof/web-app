document.addEventListener('DOMContentLoaded', async () => {
    const title = document.getElementById('status-title');
    const msg = document.getElementById('status-msg');
    const actionBtn = document.getElementById('action-btn');

    // 1. Extract query params from URL
    const params = new URLSearchParams(window.location.search);
    const token = params.get('token');
    const email = params.get('email');

    if (!token || !email) {
        showUI("Invalid Link", "The activation link is missing required parameters.", "var(--error)", "/register/register.html", "Back to Register");
        return;
    }

    try {
        // 2. Call the REST API
        const response = await fetch(`/api/activate?token=${encodeURIComponent(token)}&email=${encodeURIComponent(email)}`);
        const text = await response.text();

        // 3. Handle Branches according to HTTP status code:
        if (response.status === 200) {
            // Branch 3: Success
            showUI("Account Activated!", text, "var(--success)", "/index.html", "Proceed to Login");

        } else if (response.status === 410) {
            // Branch 2: Expired Token (HTTP 410 Gone)
            showUI("Link Expired", "Your 24-hour activation link has expired.", "var(--error)", "/register/register.html", "Resend Activation Email");

        } else {
            // Branch 1: Invalid Token or Already Active (HTTP 400)
            showUI("Activation Failed", text || "Invalid activation link or account already active.", "var(--error)", "/index.html", "Return to Home");
        }

    } catch (err) {
        showUI("Connection Error", "Could not reach server to activate account.", "var(--error)", "/index.html", "Return to Home");
    }

    function showUI(titleText, msgText, color, btnHref, btnText) {
        title.textContent = titleText;
        title.style.color = color;
        msg.textContent = msgText;

        if (btnHref && btnText) {
            actionBtn.href = btnHref;
            actionBtn.textContent = btnText;
            actionBtn.style.display = 'inline-block';
        }
    }
});