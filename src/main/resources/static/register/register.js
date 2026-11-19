document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('register-form');
    const output = document.getElementById('output');
    const emailInput = document.getElementById('emailInput');
    const pwdInput = document.getElementById('passwordInput');
    const repeatInput = document.getElementById('repeatPassword');
    const toggleBtns = document.querySelectorAll('.toggle-btn');

    // Captcha elements
    const captchaModal = document.getElementById('captcha-modal');
    const captchaPrompt = document.getElementById('captcha-prompt');
    const captchaImg = document.getElementById('captcha-img');
    const captchaInput = document.getElementById('captcha-input');
    const captchaError = document.getElementById('captcha-error');
    const captchaCancelBtn = document.getElementById('captcha-cancel-btn');
    const captchaConfirmBtn = document.getElementById('captcha-confirm-btn');
    const captchaReloadBtn = document.getElementById('captcha-reload-btn');

    let currentCaptchaId = null;

    // Rules Checklist
    const rules = {
        len: document.getElementById('rule-len'),
        upper: document.getElementById('rule-upper'),
        lower: document.getElementById('rule-lower'),
        num: document.getElementById('rule-num'),
        special: document.getElementById('rule-special')
    };

    const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

    // Toggle Password Visibility
    toggleBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            const target = document.getElementById(btn.dataset.target);
            const isPassword = target.type === 'password';
            target.type = isPassword ? 'text' : 'password';
            btn.textContent = isPassword ? 'Hide' : 'Show';
        });
    });

    // Real-Time Password Validation
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

    pwdInput.addEventListener('input', () => validatePassword(pwdInput.value));

    // Form Submit -> Opens Captcha
    form.addEventListener('submit', (e) => {
        e.preventDefault();

        if (!emailRegex.test(emailInput.value.trim())) {
            showError("Invalid email address.");
            return;
        }

        if (!validatePassword(pwdInput.value)) {
            showError("Password does not meet all security requirements.");
            return;
        }

        if (pwdInput.value !== repeatInput.value) {
            showError("Passwords do not match.");
            return;
        }

        openCaptcha();
    });

    // Fetch Captcha from Server
    async function loadNewCaptcha() {
        captchaInput.value = '';
        captchaError.textContent = '';
        captchaPrompt.textContent = 'Loading challenge...';
        captchaImg.style.opacity = '0.3';

        try {
            const res = await fetch('/api/captcha');
            const data = await res.json();

            currentCaptchaId = data.captchaId;
            captchaPrompt.textContent = data.prompt;
            captchaImg.src = data.imageBase64;
            captchaImg.style.opacity = '1';
            captchaInput.focus();
        } catch (err) {
            captchaError.textContent = 'Failed to load captcha from server.';
        }
    }

    function openCaptcha() {
        captchaModal.classList.remove('hidden');
        loadNewCaptcha();
    }

    function closeCaptcha() {
        captchaModal.classList.add('hidden');
    }

    captchaCancelBtn.addEventListener('click', closeCaptcha);
    captchaReloadBtn.addEventListener('click', loadNewCaptcha);

    captchaConfirmBtn.addEventListener('click', () => {
        const answer = captchaInput.value.trim();
        if (!answer) {
            captchaError.textContent = 'Please enter a count.';
            return;
        }

        closeCaptcha();
        submitRegistration(answer);
    });

    captchaInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            captchaConfirmBtn.click();
        }
    });

    // Send Payload
    async function submitRegistration(captchaAnswer) {
        const payload = {
            email: emailInput.value.trim(),
            password: pwdInput.value,
            nickname: document.getElementById('nickname').value,
            captchaId: currentCaptchaId,
            captchaAnswer: captchaAnswer
        };

        output.textContent = 'Submitting registration...';
        output.style.borderColor = 'var(--border)';
        output.style.color = 'var(--muted-color)';

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
                validatePassword('');
            } else {
                showError(responseText || 'Registration failed.');
            }
        } catch (err) {
            showError('Network error: ' + err);
        }
    }

    function showError(message) {
        output.textContent = 'Error: ' + message;
        output.style.borderColor = 'var(--error)';
        output.style.color = 'var(--error)';
    }
});