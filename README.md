# ₿ Previsão Bitcoin

## 🌟 Sobre o Projeto

Este projeto é uma **Aplicação Web** desenvolvida com fins educacionais que combina coleta de dados em tempo real, lógica de *backend* separada e uma interface *frontend* interativa.

Seu objetivo principal é **estimar a tendência futura do preço do Bitcoin (BTC/USDT)**, utilizando modelos matemáticos básicos baseados em análise de médias e variação percentual para prever se o valor da criptomoeda irá subir, descer ou permanecer estável.

## 🛠️ Tecnologias Utilizadas

Este projeto utiliza uma arquitetura simples de serviços separados (Java Services) e uma interface web padrão.

| Categoria | Tecnologia | Uso |
| :--- | :--- | :--- |
| **Backend/Lógica** | **Java** (JDK, HTTP Server Nativo) | Coleta de dados da API da Binance e sistema de Autenticação/Persistência. |
| **Frontend/Interface** | **HTML, CSS, JavaScript** | Estrutura e Estilização da interface. |
| **Visualização** | **Chart.js** | Biblioteca JavaScript para renderizar o gráfico interativo de preços. |
| **Persistência** | **Arquivo CSV** | Utilizado para salvar o histórico de preços (`btc_prices.csv`) e os dados de usuários (`users_db.csv`). |


## 💡 Lógica de Previsão

O *frontend* implementa dois modelos de análise para determinar a previsão:

1.  **Análise de Tendência (Variação Total):** Calcula a **Variação Percentual** do preço entre o início do histórico (últimas 12h) e o preço atual. Se a variação for superior a $\pm 0.2\%$, a tendência é marcada como Alta ou Baixa.
2.  **Forecast (Média Móvel de Três Períodos):** Divide o histórico de preços em **três blocos de tempo** e calcula a média de cada bloco. Se a sequência das médias for estritamente crescente (Média 1 < Média 2 < Média 3), é indicada uma **Tendência de Alta**; se for estritamente decrescente, uma **Tendência de Baixa**.


## ⚙️ Pré-requisitos

Para rodar o projeto, você precisa ter instalado:

* **Java Development Kit (JDK)**: Versão 11 ou superior.

## 🚀 Instalação e Execução

O projeto exige a execução de dois serviços Java em portas diferentes e a abertura da interface web.

### 1. Preparação dos Arquivos

1.  Clone o repositório ou baixe os arquivos do projeto.
2.  Certifique-se de que os arquivos `BinanceBTCPrice.java` e `bancodedados.java` estejam acessíveis.

### 2. Execução dos Backends (Java)

Você deve compilar e executar ambos os arquivos Java separadamente.

#### A. Servidor de Autenticação e Arquivos Estáticos (Porta 8001)

Este servidor é responsável pelo login, registro e por servir o `index.html`.

# Compilar:
javac bancodedados.java
# Executar:
java bancodedados


**Nota:** Este serviço criará a pasta `data/` e o arquivo `users_db.csv`.

#### B. Coletor de Dados e Histórico (Porta 8000)

Este serviço se conecta à API da Binance para obter dados em tempo real e disponibilizá-los.


# Compilar:
javac BinanceBTCPrice.java
# Executar:
java BinanceBTCPrice

**Nota:** Este serviço criará ou atualizará o arquivo de histórico de preços (`btc_prices.csv`).

### 3\. Acesso à Aplicação Web

Com ambos os servidores Java rodando:

1.  Abra o arquivo **`index.html`** diretamente no seu navegador.
2.  O JavaScript no `index.html` se conectará automaticamente aos servidores locais (`localhost:8000` e `localhost:8001`) para buscar dados e habilitar a interface.


