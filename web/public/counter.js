function unnull(x) {
    return x ? x : '';
}

function getUrlParams() {
    const params = (new URL(document.location)).searchParams;
    const serial = unnull(params.get('serial'));
    const year = unnull(params.get('year'));
    const month = unnull(params.get('month'));
    return {serial, year, month}
}

function setSelected(name, value) {
    if (value !== '') {
        const rb = document.querySelector('input[name="' + name + '"][value="' + value + '"]');
        if (rb) {
            rb.checked = true;
        }
    }
}

function showParams() {
    const params = getUrlParams();
    document.getElementById('serial').value = params.serial;
    setSelected('year', params.year);
    setSelected('month', params.month);
}

function init() {
    const yto = new Date().getFullYear();
    const yfrom = Math.max(2020, yto - 9);
    const years = document.getElementById('yearsList');
    for (let y = yfrom; y <= yto; y++) {
        const rb = document.createElement('input');
        const id = 'year' + y;
        rb.setAttribute('type', 'radio');
        rb.setAttribute('id', id);
        rb.setAttribute('name', 'year');
        rb.setAttribute('value', y.toString());

        const lbl = document.createElement('label');
        lbl.setAttribute('for', id);
        lbl.append(y.toString());

        years.append(rb);
        years.append(lbl);
    }
    showParams();
    window.addEventListener('popstate', event => {
        showParams();
        document.getElementById('result').replaceChildren();
    });
}

function escapeHtml(value) {
    const tmp = document.createElement('div');
    tmp.innerText = value;
    return tmp.innerHTML;
}

function getSelected(name) {
    const selected = document.querySelector('input[name="' + name + '"]:checked');
    return selected ? selected.value : '';
}

async function search() {
    const serialInput = document.getElementById('serial');
    const serial = serialInput.value.trim();
    if (serial === '') {
        serialInput.classList.add('emptyInput');
        serialInput.focus();
        return;
    }
    const year = getSelected('year');
    const month = getSelected('month');
    const currentParams = getUrlParams();
    if (serial !== currentParams.serial || year !== currentParams.year || month !== currentParams.month) {
        history.pushState({}, "", "/?" + new URLSearchParams({serial, year, month}));
    }
    const btn = document.getElementById('btnSearch');
    const result = document.getElementById('result');
    document.querySelectorAll('input[type="radio"]').forEach(r => r.disabled = true);
    serialInput.disabled = true;
    btn.disabled = true;
    result.innerHTML = `<h2>Поиск по заводскому номеру ${escapeHtml(serial)}... <img src="/loading.gif" width="24px" alt="Пожалуйста подождите..."></h2>`;
    let html = '<h1 class="error">Ошибка при обращении к ФГИС «Аршин»</h1>';
    try {
        console.log({serial, year, month});
        const response = await fetch('/arshin/counter/html?' + new URLSearchParams({serial, year, month}));
        if (response.ok) {
            html = await response.text();
        }
    } finally {
        document.querySelectorAll('input[type="radio"]').forEach(r => r.disabled = false);
        serialInput.disabled = false;
        btn.disabled = false;
        result.innerHTML = html;
    }
}

function resetErrors() {
    const serialInput = document.getElementById('serial');
    serialInput.classList.remove('emptyInput');
}

function keyDown(e) {
    if (e.keyCode === 13) {
        e.preventDefault();
        search();
    }
}
