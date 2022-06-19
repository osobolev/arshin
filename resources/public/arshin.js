function getUrlNum() {
    const params = (new URL(document.location)).searchParams;
    const num = params.get('num');
    return num ? num : '';
}

function showNum() {
    document.getElementById('num').value = getUrlNum();
    document.getElementById('result').replaceChildren();
}

function init() {
    showNum();
    window.addEventListener('popstate', event => {
        showNum();
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
        history.pushState({}, "", "/?" + new URLSearchParams({num}));
    }
    const btn = document.getElementById('btnSearch');
    const result = document.getElementById('result');
    numInput.disabled = true;
    btn.disabled = true;
    result.innerHTML = `<h2>Поиск по номеру в госреестре ${escapeHtml(num)}... <img src="/loading.gif" width="24px" alt="Пожалуйста подождите..."></h2>`;
    let html = '';
    try {
        const response = await fetch('/arshinHtml?' + new URLSearchParams({num}));
        if (response.status === 200) {
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
