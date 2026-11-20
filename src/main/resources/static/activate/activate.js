document.addEventListener('DOMContentLoaded', async () => {
    const iconSlot = document.getElementById('icon-slot');
    const title = document.getElementById('status-title');
    const msg = document.getElementById('status-msg');
    const actionSlot = document.getElementById('action-slot');

    // Extract query parameters
    const params = new URLSearchParams(window.location.search);
    const token = params.get('token');
    const email = params.get('email');

    if (!token || !email) {
        renderState('error', 'Missing Information', 'The activation link is incomplete or malformed.', '/register/register.html', 'Go to Registration');
        return;
    }

    try {
        // Send verification to /activate
        const response = await fetch(`/activate?token=${encodeURIComponent(token)}&email=${encodeURIComponent(email)}`);
        const responseText = await response.text();

        if (response.status === 200) {
            // --- BRANCH 3: SUCCESS ---
            renderState(
                'success',
                'Account Activated!',
                `Welcome aboard! Your account (<strong>${escapeHtml(email)}</strong>) is now verified.`,
                '/index.html',
                'Proceed to Login'
            );

        } else if (response.status === 410) {
            // --- BRANCH 2: EXPIRED LINK ---
            renderState(
                'warning',
                'Activation Link Expired',
                'Your 24-hour verification window has ended. You can request a fresh link below.',
                null,
                'Resend Activation Link',
                () => handleResend(email)
            );

        } else {
            // --- BRANCH 1: INVALID OR ALREADY ACTIVE ---
            renderState(
                'error',
                'Activation Failed',
                responseText || 'This link is invalid or your account has already been activated.',
                '/index.html',
                'Return to Home'
            );
        }

    } catch (err) {
        renderState('error', 'Connection Error', 'Could not reach the server to verify your account.', '/index.html', 'Return to Home');
    }

    // Handles the "Resend Activation Link" button click
    async function handleResend(targetEmail) {
        iconSlot.innerHTML = '<div class="spinner"></div>';
        title.textContent = 'Generating New Link...';
        title.style.color = 'var(--primary)';
        msg.textContent = 'Please wait while we create a fresh activation token.';
        actionSlot.innerHTML = '';

        try {
            const res = await fetch('/activate/resend', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email: targetEmail })
            });

            const resText = await res.text();

            renderState(
                'success',
                'Check Your Email!',
                resText || 'A brand new activation link has been sent to your inbox.',
                '/index.html',
                'Return to Home'
            );
        } catch (err) {
            renderState('error', 'Resend Failed', 'Unable to contact server. Please try again later.', null, null);
        }
    }

    // Helper to render UI states and SVGs
    function renderState(type, titleText, msgText, btnHref, btnText, btnCallback) {
        actionSlot.innerHTML = '';

        if (type === 'success') {
            iconSlot.innerHTML = `
                <div class="icon-circle state-success">
                    <svg fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7" />
                    </svg>
                </div>`;
            title.style.color = 'var(--success)';
        } else if (type === 'warning') {
            iconSlot.innerHTML = `
                <div class="icon-circle state-warning">
                    <svg fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                </div>`;
            title.style.color = '#fb923c';
        } else {
            iconSlot.innerHTML = `
                <div class="icon-circle state-error">
                    <svg fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M6 18L18 6M6 6l12 12" />
                    </svg>
                </div>`;
            title.style.color = 'var(--error)';
        }

        title.textContent = titleText;
        msg.innerHTML = msgText;

        if (btnText) {
            const btn = document.createElement(btnHref ? 'a' : 'button');
            btn.className = 'btn-action';
            btn.textContent = btnText;

            if (btnHref) {
                btn.href = btnHref;
            } else if (btnCallback) {
                btn.type = 'button';
                btn.addEventListener('click', btnCallback);
            }
            actionSlot.appendChild(btn);
        }
    }

    function escapeHtml(str) {
        return str.replace(/[&<>'"]/g, tag => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[tag] || tag));
    }
});