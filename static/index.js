console.log('index.js loaded!')

LOCK = new Set()

function download(file) {
    var a = document.createElement('a')
    a.href = file
    a.click()
}

function print_status(status) {
    console.log(status['msg'])
}

function create_icon_btn(icon) {
    let btn = document.createElement('button')
    let ic = document.createElement('i')
    ic.className = icon
    btn.appendChild(ic)
    return btn
}

function change_color_timeout(obj, color) {
    if (LOCK.has(obj)) return 0;
    LOCK.add(obj)
    old_color = obj.style.backgroundColor
    obj.style.backgroundColor = color
    setTimeout(function () {
        obj.style.backgroundColor = old_color
        LOCK.delete(obj)
    }, 300)
}

function create_crud_btn(icon, selection, callback) {
    let btn = create_icon_btn(icon)
    btn.className = 'pack-explorer-crud-btn'
    btn.addEventListener('click', function () {
        if (selection.size > 0) {
            for (let cb of document.getElementsByClassName('pack-explorer-card-cb')) {
                cb.checked = false;
            }
            selection.clear()
            callback()
        } else {
            for (let cbb of document.getElementsByClassName('pack-explorer-card-cb-box')) {
                change_color_timeout(cbb, 'red')
            }
        }
    });
    return btn
}

function bytes_to_human(bytes_n) {
    if (bytes_n > 1000000) {
        return String(Math.floor(bytes_n / 1000000)) + "MB"
    }
    if (bytes_n > 1000) {
        return String(Math.floor(bytes_n / 1000)) + "KB"
    }
    return String(bytes_n) + "B"
}

function ts_to_date(timestamp) {
    var date = new Date(timestamp * 1000)
    return date.toLocaleDateString("en-US") + " " + date.toISOString().slice(-13, -5)
}

async function fill_explorers(explorers) {
    for (let pe of explorers) {
        let selection = new Set();
        let crud = document.createElement('div')
        crud.className = 'pack-explorer-crud'

        crud.appendChild(document.createTextNode("Upload time"))

        crud.appendChild(create_crud_btn('fa fa-trash', selection, function () {
            toastr.error('Deleting currently not implemented')
            console.log('Delete not implemented')
        }));
        crud.appendChild(create_crud_btn('fas fa-edit', selection, function () {
            toastr.error('Edit currently not implemented')
            console.log('Edit not implemented')
        }));

        let clear = document.createElement('div')
        clear.style = "clear: both;"
        crud.appendChild(clear)

        pe.appendChild(crud);

        (await fetch('/uploads')).json().then(
            data => {
                for (let i = 0; i < data.length; i++) {
                    const fn = data[i]['name']
                    const size = data[i]['size']
                    const time = data[i]['time']

                    let card = document.createElement('div')
                    card.className = 'pack-explorer-card'

                    let text = document.createElement('div')
                    text.className = 'pack-explorer-card-text'
                    let sizeDiv = document.createElement('div')
                    sizeDiv.className = 'pack-explorer-card-size'
                    sizeDiv.appendChild(document.createTextNode(bytes_to_human(size)))
                    let timeDiv = document.createElement('div')
                    timeDiv.className = 'pack-explorer-card-time'
                    timeDiv.appendChild(document.createTextNode(ts_to_date(time)))
                    let text_seperator = document.createElement('div')
                    text_seperator.className = 'pack-explorer-card-text-seperator'
                    text.appendChild(timeDiv)
                    text.appendChild(text_seperator)
                    text.appendChild(document.createTextNode(' ' + fn))
                    text.appendChild(sizeDiv)

                    let cbb = document.createElement('div')
                    cbb.className = 'pack-explorer-card-cb-box'
                    let cb = document.createElement('input')
                    cb.type = 'checkbox'
                    cb.id = String(i)
                    cb.className = 'pack-explorer-card-cb'
                    cbb.appendChild(cb)

                    let seperator = document.createElement('div')
                    seperator.className = 'pack-explorer-card-seperator'

                    card.appendChild(cbb)
                    card.appendChild(seperator)
                    card.appendChild(text)

                    pe.appendChild(card)

                    cb.addEventListener('click', function () {
                        if (!selection.has(cb)) {
                            selection.add(cb);
                        }
                    })

                    text.addEventListener("click", function () {
                        if (change_color_timeout(text, '#4bb44b') == 0) {
                            return;
                        }
                        let payload = {
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/json'
                            },
                            body: JSON.stringify({ 'set': fn })
                        }
                        fetch('/pack', payload).then(r => r.json().then(dd => {
                            print_status(dd)
                            location.reload();
                        }))

                    })
                }
            }
        )
    }
}

for (let reset_btn of document.getElementsByClassName("reset-btn")) {
    reset_btn.appendChild(document.createTextNode("Reset texture pack"))
    // reset_btn.textContent = "Reset texture pack"
    reset_btn.addEventListener("click", function () {
        let payload = {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ 'set': 0 })
        }
        fetch('/pack', payload).then(r => r.json().then(dd => {
            print_status(dd)
            location.reload();
        }))
    })
}
fill_explorers(document.getElementsByClassName('pack-explorer'))
