<#-- @ftlvariable name="num" type="java.lang.String" -->
<#-- @ftlvariable name="error" type="boolean" -->
<#-- @ftlvariable name="info" type="arshin.dto.NumInfo" -->
<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <title>Публичный портал проверки средств измерений (СИ) в ФГИС «Аршин»</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>

<h1>Публичный портал проверки средств измерений (СИ) в ФГИС «Аршин»</h1>
<form method="get" action="/">
    <input type="text" size="40" name="num" placeholder="Введите регистрационный номер СИ" value="${num!}">
    <input type="submit" value="Найти">
</form>

<#if error??>
    <h1 class="error">Ошибка при обращении к ФГИС «Аршин»</h1>
</#if>

<#if num?? && info??>
    <h1>Результаты поиска по номеру в госреестре ${num}</h1>

    <#if info.regItems?has_content>

        <h2>Утвержденные типы средств измерений</h2>
        <table>
            <tr>
                <th>Наименование СИ</th>
                <th>Обозначение типа СИ</th>
                <th>Изготовитель</th>
                <th></th>
            </tr>
            <#list info.regItems as item>
                <tr>
                    <td>${item.name}</td>
                    <td>${item.type}</td>
                    <td>${item.manufacturer}</td>
                    <td><a href="${item.link}" target="_blank">Просмотреть в ФГИС «Аршин»</a></td>
                </tr>
            </#list>
        </table>

    <#else>

        <h2 class="error">Утвержденных типов средств измерений не найдено</h2>

    </#if>

    <#if info.verifyItems?has_content>

        <h2>Сведения о результатах поверки средств измерений</h2>
        <h3>Всего результатов найдено: ${info.verifyItems?size}</h3>
        <table>
            <tr>
                <th>Организация-поверитель</th>
                <th>Наименование типа СИ</th>
                <th>Тип СИ</th>
                <th>Модификация СИ</th>
                <th>Заводской номер или буквенно-цифровое обозначение</th>
                <th>Дата поверки</th>
                <th>Действ. до</th>
                <th>Номер свидетельства / извещения</th>
                <th>Пригодность</th>
                <th></th>
            </tr>
            <#list info.verifyItems as item>
                <tr>
                    <td>${item.organization}</td>
                    <td>${item.typeName}</td>
                    <td>${item.type}</td>
                    <td>${item.modification}</td>
                    <td>${item.factoryNum}</td>
                    <td>${item.verifyDate}</td>
                    <td>${item.validTo}</td>
                    <td>${item.docNum}</td>
                    <td>${item.acceptable}</td>
                    <td><a href="${item.link}" target="_blank">Просмотреть в ФГИС «Аршин»</a></td>
                </tr>
            </#list>
        </table>

    <#else>

        <h2 class="error">Сведений о результатах поверки средств измерений не найдено</h2>

    </#if>
</#if>

</body>
</html>
