let svg = undefined;
const render = x => new Viz().renderString(x).then(o => svg = o); render;