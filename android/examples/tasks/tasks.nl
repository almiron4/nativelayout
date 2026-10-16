<nativelayout>

<style>
    :root {
        --bg: #F4F5F7;
        --card: #FFFFFF;
        --line: #E5E7EB;
        --text: #111827;
        --muted: #6B7280;
        --accent: #4F46E5;
        --accent-soft: #E0E7FF;
        --danger: #DC2626;
    }

    body    { background: var(--bg); color: var(--text); padding: 1.25rem; gap: 1rem; }
    p       { margin: 0; }
    h1      { margin: 0; font-size: 1.75rem; font-weight: bold; }
    .head   { gap: 0.25rem; }
    .sub    { color: var(--muted); }
    .muted  { color: var(--muted); }

    .track  { background: var(--line); border-radius: 0.5rem; height: 0.5rem; }
    .fill   { background: var(--accent); border-radius: 0.5rem; height: 0.5rem; }

    .row    { flex-direction: row; align-items: center; gap: 0.5rem; }
    #title  { flex: 1; background: var(--card); border: 1px solid var(--line); border-radius: 0.75rem; padding: 0.875rem; }
    #add    { background: var(--accent); color: #FFFFFF; font-weight: bold; border-radius: 0.75rem; padding: 0.875rem; }

    .filters { flex-direction: row; gap: 0.5rem; }
    .chip    { background: var(--card); color: var(--muted); border: 1px solid var(--line); border-radius: 1rem; padding: 0.625rem; }
    .chip-on { background: var(--accent); color: #FFFFFF; font-weight: bold; border: 1px solid var(--accent); border-radius: 1rem; padding: 0.625rem; }

    .card   { flex-direction: row; align-items: center; gap: 0.75rem; background: var(--card); border: 1px solid var(--line); border-radius: 1rem; padding: 0.875rem; }
    .box    { width: 1.5rem; height: 1.5rem; border: 2px solid var(--accent); border-radius: 0.5rem; }
    .box-on { width: 1.5rem; height: 1.5rem; background: var(--accent); border: 2px solid var(--accent); border-radius: 0.5rem; }
    .t      { flex: 1; font-size: 1rem; }
    .t-done { flex: 1; font-size: 1rem; color: var(--muted); }
    .remove { background: transparent; color: var(--danger); font-size: 0.875rem; padding: 0.5rem; }

    .center { align-items: center; padding: 2rem; }
    .ghost  { background: var(--accent-soft); color: var(--accent); font-weight: bold; border-radius: 0.75rem; padding: 0.875rem; }
</style>

<body>
    <div class="head">
        <h1>Tasks</h1>
        <p class="sub">{pending} pending, {done} done</p>
    </div>

    <div class="track">
        <div class="fill" style="width: {percent}%"></div>
    </div>

    <div class="row">
        <input id="title" placeholder="What needs doing?">
        <button id="add">Add</button>
    </div>

    <div class="filters">
        if filter == "all":
            <button id="show-all" class="chip-on">All {total}</button>
        else:
            <button id="show-all" class="chip">All {total}</button>
        end
        if filter == "pending":
            <button id="show-pending" class="chip-on">Pending {pending}</button>
        else:
            <button id="show-pending" class="chip">Pending {pending}</button>
        end
        if filter == "done":
            <button id="show-done" class="chip-on">Done {done}</button>
        else:
            <button id="show-done" class="chip">Done {done}</button>
        end
    </div>

    if shown.size == 0:
        <div class="center">
            <p class="muted">{empty}</p>
        </div>
    else:
        for task in shown:
            <div class="card">
                if task.done:
                    <div class="box-on"></div>
                    <p class="t-done">{task.title}</p>
                else:
                    <div class="box"></div>
                    <p class="t">{task.title}</p>
                end
                <button class="remove">Remove</button>
            </div>
        end
    end

    if done > 0:
        <button id="clear" class="ghost">Clear completed</button>
    end
</body>

</nativelayout>