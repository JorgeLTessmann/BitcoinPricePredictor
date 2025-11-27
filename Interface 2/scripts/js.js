document.addEventListener('DOMContentLoaded', () => {
    const registerForm = document.getElementById('registerForm');

    if (registerForm) {
        registerForm.addEventListener('submit', async function (e) {
            e.preventDefault();

            const btn = this.querySelector('button[type="submit"]');
            const originalText = btn.textContent;

            const email = document.getElementById('email').value;
            const username = document.getElementById('username').value;
            const password = document.getElementById('password').value;

            btn.textContent = 'Cadastrando...';
            btn.disabled = true;


            const formData = new URLSearchParams();
            formData.append('email', email);
            formData.append('username', username); 
            formData.append('password', password);

            try {
                const response = await fetch('http://localhost:8001/register', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded'
                    },

                    body: formData.toString() 
                });

                const data = await response.json(); 

                if (response.ok && data.success) {
                    alert('Cadastro realizado com sucesso! Agora você pode fazer login.');
                    window.location.href = 'login.html';
                } else {
                    alert('Erro no cadastro: ' + (data.error || 'Erro desconhecido.'));
                }
            } catch (error) {
                console.error('Erro ao conectar com o servidor:', error);
                alert('Erro ao conectar com o servidor para o cadastro.');
            } finally {
                btn.textContent = originalText;
                btn.disabled = false;
            }
        });
    }
});



const menuToggle = document.querySelector('.menu-toggle');
const menu = document.querySelector('.menu');


const toggleMenu = () => {
    const isExpanded = menuToggle.getAttribute('aria-expanded') === 'true';
    menuToggle.setAttribute('aria-expanded', !isExpanded);

    if (!isExpanded) {
        menu.style.display = 'flex';
        setTimeout(() => menu.classList.add('active'), 10);
    } else {
        menu.classList.remove('active');
        setTimeout(() => menu.style.display = 'none', 400);
    }
};

const closeMenuOnLinkClick = () => {
    if (window.innerWidth <= 768) {
        menu.classList.remove('active');
        menuToggle.setAttribute('aria-expanded', 'false');
        setTimeout(() => menu.style.display = 'none', 400);
    }
};

const handleResize = () => {
    if (window.innerWidth > 768) {
        menu.style.display = 'flex';
        menu.classList.remove('active');
        menuToggle.setAttribute('aria-expanded', 'false');
    } else if (!menu.classList.contains('active')) {
        menu.style.display = 'none';
    }
};

document.getElementById('loginForm').addEventListener('submit', async function (e) {
    e.preventDefault();

    const btn = this.querySelector('button[type="submit"]');
    const originalText = btn.textContent;
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;

    btn.textContent = 'Entrando...';
    btn.disabled = true;

    try {
        const response = await fetch('http://localhost:8001/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: `email=${encodeURIComponent(email)}&password=${encodeURIComponent(password)}`
        });

        const data = await response.json();

        if (data.success) {
            localStorage.setItem('authToken', data.token);
            alert('Login realizado com sucesso!');
            window.location.href = '../index.html'; 
        } else {
            alert('Erro: Credenciais inválidas');
        }
    } catch (error) {
        console.error('Erro:', error);
        alert('Erro ao conectar com o servidor');
    } finally {
        btn.textContent = originalText;
        btn.disabled = false;
    }
});


