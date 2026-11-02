import * as echarts from 'echarts';

// CU-19 Dashboard de gráficas: los colores se definen una sola vez, en este tema de
// ECharts. Ninguna gráfica define colores por su cuenta.

// Paleta pastel para las series categóricas
export const PALETA_PASTEL = [
  '#A8D8EA', '#FFD6A5', '#CAFFBF', '#FFADAD', '#BDB2FF',
  '#FDFFB6', '#9BF6FF', '#FFC6FF', '#D0F4DE', '#E4C1F9'
];

// Escala secuencial de un solo tono pastel, para el mapa de calor y el treemap
export const ESCALA_SECUENCIAL = ['#EEF7FB', '#7FB7D4'];

// Semáforo del gauge y de la tabla de Técnicos
export const SEMAFORO_VERDE = '#B5EAD7';
export const SEMAFORO_AMARILLO = '#FFF1B6';
export const SEMAFORO_ROJO = '#FFB7B2';

// Texto, etiquetas y bordes: el pastel necesita contraste para leerse
export const TINTA = '#2F3640';
export const BORDE = '#2F3640';

export const NOMBRE_TEMA = 'pastel';

let registrado = false;

export function registrarTemaPastel(): void {
  if (registrado) {
    return;
  }
  registrado = true;

  echarts.registerTheme(NOMBRE_TEMA, {
    color: PALETA_PASTEL,
    backgroundColor: 'transparent',
    textStyle: { color: TINTA, fontFamily: 'Work Sans, system-ui, sans-serif' },
    title: {
      textStyle: { color: TINTA, fontSize: 15, fontWeight: 600 },
      subtextStyle: { color: TINTA }
    },
    legend: { textStyle: { color: TINTA } },
    tooltip: {
      backgroundColor: '#ffffff',
      borderColor: BORDE,
      borderWidth: 1,
      textStyle: { color: TINTA }
    },
    categoryAxis: {
      axisLine: { lineStyle: { color: BORDE } },
      axisTick: { lineStyle: { color: BORDE } },
      axisLabel: { color: TINTA },
      splitLine: { show: false }
    },
    valueAxis: {
      axisLine: { lineStyle: { color: BORDE } },
      axisTick: { lineStyle: { color: BORDE } },
      axisLabel: { color: TINTA },
      splitLine: { lineStyle: { color: '#e5e0d5' } }
    },
    // Bordes finos más oscuros, para que las barras y los sectores se separen
    bar: { itemStyle: { borderColor: BORDE, borderWidth: 1 } },
    pie: {
      itemStyle: { borderColor: BORDE, borderWidth: 1 },
      label: { color: TINTA }
    },
    funnel: { itemStyle: { borderColor: BORDE, borderWidth: 1 } },
    treemap: { itemStyle: { borderColor: BORDE, borderWidth: 1 } },
    sunburst: { itemStyle: { borderColor: BORDE, borderWidth: 1 } },
    boxplot: { itemStyle: { borderColor: BORDE, borderWidth: 1 } },
    sankey: { itemStyle: { borderColor: BORDE, borderWidth: 1 } },
    // Líneas de 3 px con marcadores con borde
    line: {
      lineStyle: { width: 3 },
      symbol: 'circle',
      symbolSize: 8,
      itemStyle: { borderColor: BORDE, borderWidth: 1 }
    },
    scatter: { itemStyle: { borderColor: BORDE, borderWidth: 1 } },
    radar: {
      lineStyle: { width: 3 },
      symbol: 'circle',
      symbolSize: 8,
      itemStyle: { borderColor: BORDE, borderWidth: 1 }
    },
    gauge: {
      title: { color: TINTA },
      detail: { color: TINTA },
      axisLabel: { color: TINTA }
    },
    visualMap: { textStyle: { color: TINTA } }
  });
}
