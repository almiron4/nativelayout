<nativelayout>

<link rel="stylesheet" href="stress.css">

<body>
    <h1>Teste de carga</h1>
    <p class="muted">{total} itens, cerca de {nos} nós</p>

    <div class="chips">
        for t in tamanhos:
            if t.ativo:
                <button class="chip chip-on">{t.nome}</button>
            else:
                <button class="chip">{t.nome}</button>
            end
        end
    </div>

    <div class="bar">
        <button id="rolar" class="act">Rolar (FPS)</button>
        <button id="mudar" class="act">Mudar 1</button>
        <button id="bench" class="act">Core</button>
    </div>

    for x in itens:
        <div class="item">
            <div class="thumb"><p class="thumb-t">{x.inicial}</p></div>
            <div class="grow">
                <p class="name">{x.nome}</p>
                <p class="muted">{x.desc}</p>
                <p class="price">{x.preco}</p>
            </div>
            if x.qtd > 0:
                <div class="qtd"><p class="qtd-t">{x.qtd}</p></div>
            end
            <button class="mais">+</button>
        </div>
    end
</body>

</nativelayout>
