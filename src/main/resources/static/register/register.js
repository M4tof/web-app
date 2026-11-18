document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('register-form');
    const output = document.getElementById('output');
    const emailInput = document.getElementById('emailInput');
    const pwdInput = document.getElementById('passwordInput');
    const repeatInput = document.getElementById('repeatPassword');
    const toggleBtns = document.querySelectorAll('.toggle-btn');

    // Captcha elements
    const captchaModal = document.getElementById('captcha-modal');
    const captchaDisplay = document.getElementById('captcha-display');
    const captchaInput = document.getElementById('captcha-input');
    const captchaError = document.getElementById('captcha-error');
    const captchaCancelBtn = document.getElementById('captcha-cancel-btn');
    const captchaConfirmBtn = document.getElementById('captcha-confirm-btn');

    // DOM references for checklist items
    const rules = {
        len: document.getElementById('rule-len'),
        upper: document.getElementById('rule-upper'),
        lower: document.getElementById('rule-lower'),
        num: document.getElementById('rule-num'),
        special: document.getElementById('rule-special')
    };

    const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

    // 1. Password Visibility Toggle
    toggleBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            const target = document.getElementById(btn.dataset.target);
            const isPassword = target.type === 'password';
            target.type = isPassword ? 'text' : 'password';
            btn.textContent = isPassword ? 'Hide' : 'Show';
        });
    });

    // 2. Real-Time Password Validation
    function validatePassword(val) {
        const checks = {
            len: val.length >= 8 && val.length <= 64,
            upper: /[A-Z]/.test(val),
            lower: /[a-z]/.test(val),
            num: /[0-9]/.test(val),
            special: /[@$!%*?&#^()_+\-=\[\]{}|;:,.<>]/.test(val)
        };

        for (const [key, passed] of Object.entries(checks)) {
            rules[key].classList.toggle('valid', passed);
        }

        return Object.values(checks).every(Boolean);
    }

    pwdInput.addEventListener('input', () => {
        validatePassword(pwdInput.value);
    });

    // 3. Form Submit -> Opens Captcha Modal
    form.addEventListener('submit', (e) => {
        e.preventDefault();

        const email = emailInput.value.trim();
        const password = pwdInput.value;
        const repeatPassword = repeatInput.value;

        output.style.borderColor = 'var(--border)';
        output.style.color = 'var(--muted-color)';

        if (!emailRegex.test(email)) {
            showError("Invalid email address. It must include a domain (e.g. name@domain.com).");
            return;
        }

        if (!validatePassword(password)) {
            showError("Password does not meet all security requirements.");
            return;
        }

        if (password !== repeatPassword) {
            showError("Passwords do not match.");
            return;
        }

        // All checks passed -> Show the Captcha popup!
        openCaptcha();
    });

    // --- Captcha Modal Logic ---
    function openCaptcha() {
        captchaInput.value = '';
        captchaError.textContent = '';
        captchaModal.classList.remove('hidden');
        captchaInput.focus();
    }

    function closeCaptcha() {
        captchaModal.classList.add('hidden');
    }

    captchaCancelBtn.addEventListener('click', closeCaptcha);

    captchaConfirmBtn.addEventListener('click', () => {
        const enteredVal = captchaInput.value.trim();
        const expectedVal = captchaDisplay.textContent.trim(); // "4"

        if (enteredVal !== expectedVal) {
            captchaError.textContent = "Incorrect code, please try again.";
            captchaInput.value = '';
            captchaInput.focus();
            return;
        }

        // Correct! Close modal and send payload
        closeCaptcha();
        submitRegistration();
    });

    // Allow pressing "Enter" inside captcha input
    captchaInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            captchaConfirmBtn.click();
        }
    });

    // 4. Send Payload to Server
    async function submitRegistration() {
        const payload = {
            email: emailInput.value.trim(),
            password: pwdInput.value,
            nickname: document.getElementById('nickname').value
        };

        output.textContent = "Sending registration to server...";

        try {
            const res = await fetch('/api/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const responseText = await res.text();

            if (res.ok) {
                output.style.borderColor = 'var(--success)';
                output.style.color = 'var(--success)';
                output.textContent = responseText;
                form.reset();
                validatePassword("");
            } else {
                showError(responseText || "Server rejected registration.");
            }
        } catch (err) {
            showError("Network error communicating with server: " + err);
        }
    }

    function showError(message) {
        output.textContent = "Error: " + message;
        output.style.borderColor = 'var(--error)';
        output.style.color = 'var(--error)';
    }
});