document.addEventListener('DOMContentLoaded', () => {
    const perfilLink = document.getElementById("perfil-link");

    if (!perfilLink) {
        console.warn("Elemento com ID 'perfil-link' não encontrado nesta página. O script pode não ser aplicável aqui.");
        return;
    }

    perfilLink.addEventListener("click", async (e) => {
        e.preventDefault();

        const token = localStorage.getItem("authToken");

        if (!token) {
            window.location.href = "login.html";
            return;
        }

        try {
            const res = await fetch("http://localhost:8001/profile", {
                headers: {
                    Authorization: token
                }
            });

            if (res.ok) {
                window.location.href = "criaPerfil.html";
            } else {

                console.warn("Token de autenticação inválido ou expirado. Redirecionando para login.");
                window.location.href = "login.html";
            }
        } catch (err) {
            console.error("Erro ao verificar autenticação com o servidor:", err);
            window.location.href = "login.html";
        }
    });
});


const menuToggle = document.querySelector('.menu-toggle');
const menu = document.querySelector('.menu');

menuToggle.addEventListener('click', () => {
    const isExpanded = menuToggle.getAttribute('aria-expanded') === 'true';
    menuToggle.setAttribute('aria-expanded', !isExpanded);

    if (!isExpanded) {
        menu.style.display = 'flex';
        setTimeout(() => menu.classList.add('active'), 10);
    } else {
        menu.classList.remove('active');
        setTimeout(() => menu.style.display = 'none', 400);
    }
});


document.querySelectorAll('.menu a').forEach(link => {
    link.addEventListener('click', () => {
        if (window.innerWidth <= 768) {
            menu.classList.remove('active');
            menuToggle.setAttribute('aria-expanded', 'false');
            setTimeout(() => menu.style.display = 'none', 400);
        }
    });
});

window.addEventListener('resize', () => {
    if (window.innerWidth > 768) {
        menu.style.display = 'flex';
        menu.classList.remove('active');
        menuToggle.setAttribute('aria-expanded', 'false');
    } else if (!menu.classList.contains('active')) {
        menu.style.display = 'none';
    }
});


if (window.innerWidth <= 768) {
    menu.style.display = 'none';
}