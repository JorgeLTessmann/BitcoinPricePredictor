function continuarComoVisitante() {
    document.getElementById("welcome").style.display = "none";
    initChart();
    updateData();
    setInterval(updateData, 1000);
}

window.addEventListener("DOMContentLoaded", async () => {
    const token = localStorage.getItem("authToken");

    if (token) {
        try {
            const response = await fetch("http://localhost:8001/profile", {
                headers: { Authorization: token }
            });

            if (response.ok) {
                document.getElementById("welcome").style.display = "none";
                initChart();
                updateData();
                setInterval(updateData, 1000);
                return;
            }
        } catch (err) {
            console.warn("Erro ao validar token:", err);
        }
    }


    document.getElementById("welcome").style.display = "flex";
});

let bitcoinChart;

function initChart() {
    const ctx = document.getElementById('bitcoinChart').getContext('2d');
    bitcoinChart = new Chart(ctx, {
        type: 'line',
        data: {
            datasets: [{
                label: 'BTC/USDT (Últimas 12h)',
                borderColor: 'rgb(255, 159, 64)',
                backgroundColor: 'rgba(255, 159, 64, 0.1)',
                fill: true,
                tension: 0.4,
                borderWidth: 2,
                pointRadius: 0
            }]
        },
        options: {
            animation: { duration: 0 },
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    labels: {
                        color: '#fff',
                        font: { size: 14 }
                    }
                },
                title: {
                    display: true,
                    text: 'Variação do Bitcoin (Últimas 12 horas)',
                    color: '#fff',
                    font: { size: 16, weight: 'bold' }
                }
            },
            scales: {
                x: {
                    type: 'time',
                    time: { unit: 'hour', tooltipFormat: 'HH:mm', displayFormats: { hour: 'HH:mm' } },
                    grid: { color: '#444' },
                    ticks: { color: '#ccc' }
                },
                y: {
                    grid: { color: '#444' },
                    ticks: {
                        color: '#ccc',
                        callback: value => '$' + value.toLocaleString('pt-BR')
                    }
                }
            }
        }
    });
}

async function updateData() {
    try {
        const [price, history] = await Promise.all([
            fetch('http://localhost:8000/btcprice').then(res => res.json()),
            fetch('http://localhost:8000/history').then(res => res.json())
        ]);

        document.getElementById('current-price').textContent =
            parseFloat(price.price).toLocaleString('pt-BR', { style: 'currency', currency: 'USD' });

        document.getElementById('last-update').textContent =
            new Date().toLocaleTimeString('pt-BR');

        const last12HoursData = history.filter(item =>
            new Date(item.timestamp) >= new Date(Date.now() - 12 * 60 * 60 * 1000)
        );

        bitcoinChart.data.labels = last12HoursData.map(item => new Date(item.timestamp));
        bitcoinChart.data.datasets[0].data = last12HoursData.map(item => parseFloat(item.price));
        bitcoinChart.update();

        updateTrend(history);
        updateForecast(last12HoursData);
    } catch (error) {
        console.error("Erro ao buscar dados:", error);
    }
}

function updateTrend(data) {
    if (data.length < 2) return;

    const latest = parseFloat(data[data.length - 1].price);
    const oldest = parseFloat(data[0].price);
    const variation = ((latest - oldest) / oldest) * 100;

    let trendClass = "estavel";
    let trendText = `Sem grande variação (${variation.toFixed(2)}%)`;

    if (variation >= 0.2) {
        trendClass = "para-alta";
        trendText = `Alta (${variation.toFixed(2)}%)`;
    } else if (variation <= -0.2) {
        trendClass = "para-baixa";
        trendText = `Baixa (${variation.toFixed(2)}%)`;
    }

    const indicator = document.getElementById("trend-indicator");
    const description = document.getElementById("trend-description");

    indicator.className = `trend-indicator ${trendClass}`;
    indicator.textContent = trendText;
    description.textContent = trendText;
}

function updateForecast(data) {
    if (data.length < 3) return;

    const blockSize = Math.floor(data.length / 3);
    const blocks = [
        data.slice(0, blockSize),
        data.slice(blockSize, 2 * blockSize),
        data.slice(2 * blockSize)
    ];

    const avgs = blocks.map(block =>
        block.reduce((sum, item) => sum + parseFloat(item.price), 0) / block.length
    );

    let forecastClass = "estavel";
    let forecastText = `Sem tendência clara (médias: ${avgs.map(a => a.toFixed(2)).join(', ')})`;

    if (avgs[0] < avgs[1] && avgs[1] < avgs[2]) {
        forecastClass = "para-alta";
        forecastText = `Tendência de alta (médias: ${avgs.map(a => a.toFixed(2)).join(', ')})`;
    } else if (avgs[0] > avgs[1] && avgs[1] > avgs[2]) {
        forecastClass = "para-baixa";
        forecastText = `Tendência de baixa (médias: ${avgs.map(a => a.toFixed(2)).join(', ')})`;
    }

    const indicator = document.getElementById("forecast-indicator");
    const description = document.getElementById("forecast-description");

    indicator.className = `trend-indicator ${forecastClass}`;
    indicator.textContent = forecastText;
    description.textContent = forecastText;
}

document.querySelector('.menu-toggle').addEventListener('click', () => {
    document.querySelector('.menu').classList.toggle('active');
});

document.getElementById("perfil-link").addEventListener("click", async (e) => {
    e.preventDefault();

    const token = localStorage.getItem("authToken");

    if (!token) {
        window.location.href = "pages/login.html";
        return;
    }

    try {
        const res = await fetch("http://localhost:8001/profile", {
            headers: {
                Authorization: token
            }
        });

        if (res.ok) {
            window.location.href = "pages/criaPerfil.html";
        } else {
            window.location.href = "pages/login.html";
        }
    } catch (err) {
        console.warn("Erro ao verificar autenticação:", err);
        window.location.href = "pages/login.html";
    }
});
