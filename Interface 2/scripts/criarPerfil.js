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

menuToggle.addEventListener('click', toggleMenu);
menu.querySelectorAll('a').forEach(link => {
    link.addEventListener('click', closeMenuOnLinkClick);
});
window.addEventListener('resize', handleResize);
handleResize(); 

const API_BASE_URL = 'http://localhost:8001';

document.addEventListener('DOMContentLoaded', async () => {
    const authToken = localStorage.getItem('authToken');
    const profileLink = document.getElementById('profileLink');
    const loginLink = document.getElementById('loginLink');
    const logoutButton = document.getElementById('logoutButton');

    if (authToken) {
        if (profileLink) profileLink.style.display = 'block';
        if (loginLink) loginLink.style.display = 'none';
        if (logoutButton) logoutButton.style.display = 'block';
    } else {
        if (profileLink) profileLink.style.display = 'none';
        if (loginLink) loginLink.style.display = 'block';
        if (logoutButton) logoutButton.style.display = 'none';
    }

    if (!authToken) {
        alert('Você precisa estar logado para acessar esta página.');
        window.location.href = 'login.html';
        return;
    }


    if (logoutButton) {
        logoutButton.addEventListener('click', () => {
            localStorage.removeItem('authToken');
            localStorage.removeItem('userEmail');
            alert('Você foi desconectado.');
            window.location.href = 'login.html';
        });
    }

    await fetchUserProfile(authToken);
});

async function fetchUserProfile(token) {
    try {
        const response = await fetch(`${API_BASE_URL}/profile`, {
            method: 'GET',
            headers: {
                'Authorization': token,
                'Content-Type': 'application/json'
            }
        });

        const data = await response.json();

        if (response.ok && data.success) {
            document.getElementById('userEmail').textContent = data.email || 'N/A';
            document.getElementById('username').value = data.username || '';

            document.getElementById('profileAvatar').src = '../images/BENEFICIOS-SEM-PERFIL-DE-MOTORISTA-279x300.webp';
        } else {
            alert('Erro ao carregar perfil: ' + (data.error || 'Erro desconhecido'));
            console.error('Erro ao carregar perfil:', data);
            if (response.status === 401) {
                localStorage.removeItem('authToken');
                localStorage.removeItem('userEmail');
                window.location.href = 'login.html';
            }
        }
    } catch (error) {
        console.error('Erro ao conectar com o servidor de perfil:', error);
        alert('Erro ao conectar com o servidor para carregar o perfil.');
    }
}

document.getElementById('profileForm').addEventListener('submit', async function (e) {
    e.preventDefault();

    const btn = this.querySelector('button[type="submit"]');
    const originalText = btn.textContent;
    const authToken = localStorage.getItem('authToken');
    const newUsername = document.getElementById('username').value;

    if (!authToken) {
        alert('Você não está autenticado.');
        return;
    }

    btn.textContent = 'Salvando...';
    btn.disabled = true; 

    try {

        const formData = new URLSearchParams();
        formData.append('username', newUsername); 

        const response = await fetch(`${API_BASE_URL}/update-profile`, {
            method: 'POST',
            headers: {
                'Authorization': authToken,
                'Content-Type': 'application/x-www-form-urlencoded' 
            },
            body: formData.toString()
        });

        const data = await response.json(); 

        if (data.success) {
            alert('Perfil atualizado com sucesso!');
            await fetchUserProfile(authToken);
        } else {
            alert('Erro ao atualizar perfil: ' + (data.error || 'Erro desconhecido'));
        }
    } catch (error) {
        console.error('Erro ao conectar com o servidor:', error);
        alert('Erro ao conectar com o servidor para atualizar o perfil.');
    } finally {
        btn.textContent = originalText;
        btn.disabled = false; 
    }
});