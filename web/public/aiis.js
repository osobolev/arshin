function getUrlNum() {
    const params = (new URL(document.location)).searchParams;
    const num = params.get('num');
    return num ? num : '';
}

function showNum() {
    document.getElementById('num').value = getUrlNum();
}

function init() {
    showNum();
    window.addEventListener('popstate', event => {
        showNum();
        document.getElementById('result').replaceChildren();
    });
}

function escapeHtml(value) {
    const tmp = document.createElement('div');
    tmp.innerText = value;
    return tmp.innerHTML;
}

async function search() {
    const numInput = document.getElementById('num');
    const num = numInput.value.trim();
    if (num === '') {
        numInput.classList.add('emptyInput');
        numInput.focus();
        return;
    }
    if (num !== getUrlNum()) {
        history.pushState({}, "", "/aiis.html?" + new URLSearchParams({num}));
    }
    const btn = document.getElementById('btnSearch');
    const result = document.getElementById('result');
    numInput.disabled = true;
    btn.disabled = true;
    result.innerHTML = `<h2>Поиск по номеру в госреестре ${escapeHtml(num)}... <img src="/loading.gif" width="24px" alt="Пожалуйста подождите..."></h2>`;
    let html = '<h1 class="error">Ошибка при обращении к ФГИС «Аршин»</h1>';
    try {
        const response = await fetch('/arshin/aiis/html?' + new URLSearchParams({num}));
        if (response.ok) {
            html = await response.text();
        }
    } finally {
        numInput.disabled = false;
        btn.disabled = false;
        result.innerHTML = html;
    }
}

function resetErrors() {
    const numInput = document.getElementById('num');
    numInput.classList.remove('emptyInput');
}

function keyDown(e) {
    if (e.keyCode === 13) {
        e.preventDefault();
        search();
    }
}
