<nativelayout>

<link rel="stylesheet" href="menu.css">

<body>
    <div class="bar">
        <div class="grow">
            <p class="muted">{saudacao}</p>
            <h1>Café Aurora</h1>
        </div>
        <div class="avatar"><p class="avatar-t">{usuario.inicial}</p></div>
    </div>

    <div class="chips">
        for c in categorias:
            if c.ativo:
                <button class="chip chip-on">{c.nome}</button>
            else:
                <button class="chip">{c.nome}</button>
            end
        end
    </div>

    <div class="hero">
        <p class="hero-k">DESTAQUE DA CASA</p>
        <p class="hero-t">{destaque.nome}</p>
        <p>{destaque.descricao}</p>
        <div class="bar">
            <p class="hero-p grow">{destaque.preco}</p>
            <button id="pedir">Adicionar</button>
        </div>
    </div>

    <div class="bar">
        <h2 class="grow">{secao}</h2>
        <p class="muted">{n_itens} no menu</p>
    </div>

    if produtos.size == 0:
        <p class="muted">Nada nesta categoria por agora.</p>
    else:
        for prod in produtos:
            <div class="item">
                <div class="thumb"><p class="thumb-t">{prod.inicial}</p></div>
                <div class="grow">
                    <p class="name">{prod.nome}</p>
                    <p class="muted">{prod.descricao}</p>
                    <p class="price">{prod.preco}</p>
                </div>
                if prod.qtd > 0:
                    <div class="qtd"><p class="qtd-t">{prod.qtd}</p></div>
                end
                <button class="mais">+</button>
            </div>
        end
    end

    <div class="cart">
        <h2>O teu pedido</h2>
        if itens == 0:
            <p class="muted">Ainda não adicionaste nada. Toca em + para começar.</p>
        else:
            <div class="bar">
                <p class="muted grow">{itens_txt}</p>
                <p class="total">{total}</p>
            </div>
            <button id="finalizar">Finalizar pedido</button>
            <button id="limpar">Limpar</button>
        end
    </div>

    if enviado:
        <div class="snack"><p class="snack-t">Pedido enviado. Obrigado!</p></div>
    end
</body>

</nativelayout>
