/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.example.testelayou

val sizes = listOf(10, 100, 500, 1000, 2000)

fun stressData(n: Int, qtd: IntArray): Map<String, Any?> = mapOf(
    "total" to n,
    "nos" to n * 8,
    "tamanhos" to sizes.map { mapOf("nome" to it.toString(), "ativo" to (it == n)) },
    "itens" to List(n) { i ->
        mapOf(
            "inicial" to ('A' + i % 26).toString(),
            "nome" to "Item ${i + 1}",
            "desc" to "Descrição do item número ${i + 1}",
            "preco" to "${1 + i % 9},${"%02d".format((i * 7) % 100)} €",
            "qtd" to qtd[i]
        )
    }
)

private val menuItems = listOf(
    arrayOf("Espresso", "Intenso e curto", "1,50 €"),
    arrayOf("Cappuccino", "Leite vaporizado e cacau", "2,80 €"),
    arrayOf("Cold brew", "Infusão a frio, 12 horas", "3,20 €"),
    arrayOf("Iced latte", "Com gelo e leite", "3,40 €"),
    arrayOf("Pastel de nata", "Quente, com canela", "1,30 €"),
    arrayOf("Cheesecake", "Frutos vermelhos", "3,90 €")
)

fun menuData(q: Int): Map<String, Any?> = mapOf(
    "saudacao" to "Bom dia",
    "usuario" to mapOf("inicial" to "A"),
    "categorias" to listOf("Todos", "Quentes", "Frios", "Doces")
        .mapIndexed { i, nome -> mapOf("nome" to nome, "ativo" to (i == 0)) },
    "destaque" to mapOf("nome" to "Cappuccino", "descricao" to "Leite vaporizado e cacau", "preco" to "2,80 €"),
    "secao" to "Todo o menu",
    "n_itens" to 6,
    "produtos" to menuItems.mapIndexed { i, p ->
        mapOf("inicial" to p[0].take(1), "nome" to p[0], "descricao" to p[1], "preco" to p[2], "qtd" to (if (i == 1) q else 0))
    },
    "itens" to q,
    "itens_txt" to "$q itens",
    "total" to "0,00 €",
    "enviado" to false
)
