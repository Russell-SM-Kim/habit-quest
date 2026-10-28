// 디자인 시안(.dc.html)을 브라우저에서 바로 볼 수 있게 해주는 작은 런타임.
// <x-dc> 템플릿 + <script type="text/x-dc"> 데이터 → {{값}}, <sc-for>, <sc-if> 를 채워서 그린다.
(function () {
  class DCLogic { constructor(props) { this.props = props; } }
  window.DCLogic = DCLogic;

  function evaluate(expr, scope) {
    const keys = Object.keys(scope).filter(k => /^[A-Za-z_$][\w$]*$/.test(k) && k !== 'true' && k !== 'false');
    try { return Function(...keys, 'return (' + expr + ');')(...keys.map(k => scope[k])); }
    catch (e) { return ''; }
  }
  function interpolate(text, scope) {
    const whole = text.match(/^\s*\{\{([^}]+)\}\}\s*$/);
    if (whole) return evaluate(whole[1], scope);
    return text.replace(/\{\{([^}]+)\}\}/g, (_, e) => { const v = evaluate(e, scope); return v == null ? '' : v; });
  }
  function render(node, scope, out) {
    if (node.nodeType === 3) { out.appendChild(document.createTextNode(String(interpolate(node.textContent, scope)))); return; }
    if (node.nodeType !== 1) return;
    const tag = node.tagName.toLowerCase();
    if (tag === 'sc-for') {
      const list = interpolate(node.getAttribute('list'), scope) || [];
      const as = node.getAttribute('as');
      list.forEach((item, i) => node.childNodes.forEach(c => render(c, Object.assign({}, scope, { [as]: item, index: i }), out)));
      return;
    }
    if (tag === 'sc-if') {
      if (interpolate(node.getAttribute('value'), scope)) node.childNodes.forEach(c => render(c, scope, out));
      return;
    }
    if (tag === 'helmet') { node.childNodes.forEach(c => document.head.appendChild(c.cloneNode(true))); return; }
    const ns = node.namespaceURI;
    const el = ns && ns !== 'http://www.w3.org/1999/xhtml' ? document.createElementNS(ns, node.tagName) : document.createElement(node.tagName);
    for (const a of node.attributes) el.setAttribute(a.name, String(interpolate(a.value, scope)));
    node.childNodes.forEach(c => render(c, scope, el));
    out.appendChild(el);
  }
  document.addEventListener('DOMContentLoaded', () => {
    const script = document.querySelector('script[type="text/x-dc"]');
    let vals = {};
    if (script) {
      const meta = JSON.parse(script.getAttribute('data-props') || '{}');
      const props = {};
      for (const k in meta) if (k[0] !== '$') props[k] = meta[k].default;
      const Component = Function('DCLogic', script.textContent + '\nreturn Component;')(DCLogic);
      vals = new Component(props).renderVals();
    }
    const tpl = document.querySelector('x-dc');
    const host = document.createElement('div');
    tpl.childNodes.forEach(c => render(c, vals, host));
    tpl.replaceWith(...host.childNodes);
  });
})();
