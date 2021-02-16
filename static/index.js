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
        if (selection.length > 0) {
            for (let cb of document.getElementsByClassName('pack-explorer-card-cb')) {
                cb.checked = false;
            }
            callback()
        } else {
            for (let cb of document.getElementsByClassName('pack-explorer-card-cb-box')) {
                change_color_timeout(cb, 'red')
            }
        }
    });
    return btn
}

async function fill_explorers(explorers) {
    for (let pe of explorers) {
        let selection = [];
        let crud = document.createElement('div')
        crud.className = 'pack-explorer-crud'

        crud.appendChild(create_crud_btn('fa fa-trash', selection, function () {
            selection = []
            console.log('NOT IMPLEMENTED')
        }));
        crud.appendChild(create_crud_btn('fas fa-edit', selection, function () {
            selection = []
            console.log('NOT IMPLEMENTED')
        }));

        let clear = document.createElement('div')
        clear.style = "clear: both;"
        crud.appendChild(clear)

        pe.appendChild(crud);

        (await fetch('/uploads')).json().then(
            data => {
                for (let i = 0; i < data.length; i++) {
                    const fn = data[i];

                    let card = document.createElement('div')
                    card.className = 'pack-explorer-card'

                    let text = document.createElement('div')
                    text.className = 'pack-explorer-card-text'
                    text.appendChild(document.createTextNode(fn))

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
                        for (let i = 0; i < selection.length; i++) {
                            const selcb = selection[i];
                            if (selcb == cb) {
                                selection.splice(i, 1)
                                return;
                            }
                        }
                        selection.push(cb);
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

fill_explorers(document.getElementsByClassName('pack-explorer'))