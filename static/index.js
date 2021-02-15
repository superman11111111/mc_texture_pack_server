console.log('index.js loaded!')

function download(file) {
    var a = document.createElement('a')
    a.href = file
    a.click()
}

function print_status(status) {
    console.log(status['msg'])
}

async function fill_explorers(explorers) {
    for (let pe of explorers) {
        (await fetch('/uploads')).json().then(
            data => {
                for (let i = 0; i < data.length; i++) {
                    const fn = data[i];

                    let card = document.createElement('div')
                    card.className = 'pack-explorer-card'

                    let text = document.createElement('div')
                    text.appendChild(document.createTextNode(fn))

                    let cb = document.createElement('input')
                    cb.type = 'checkbox'
                    cb.id = String(i)
                    cb.className = 'pack-explorer-card-cb'

                    card.appendChild(text)
                    card.appendChild(cb)
                    pe.appendChild(card)
                    cb.addEventListener('click', function () {
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
                        // ().json().then(
                        //     dd => console.log(dd)
                        // )
                    })
                    text.addEventListener("click", function () {
                        // download('/uploads/' + fn)
                    })
                    text.addEventListener("click", function () {
                        old_color = card.style.backgroundColor
                        card.style.backgroundColor = 'red'
                        setTimeout(function () {
                            card.style.backgroundColor = old_color
                        }, 300)
                    })
                }
            }
        )
    }
}

fill_explorers(document.getElementsByClassName('pack-explorer'))