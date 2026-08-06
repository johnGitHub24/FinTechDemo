/**
 * 【職責】保存最新 demoTrace、拓撲燈、可釘住的敘事 S 階。
 * 【技巧】與 auth store 相同 reactive 模組模式（非 pinia）。
 * 【概念】Trade／Portal 共用同一「後端故事」狀態。
 */
import { reactive, computed } from 'vue';
import { fetchTopology } from '../api/client';
import { mergeTrace } from '../demo/mergeTrace.js';
import { inferStage } from '../demo/inferStage.js';

const state = reactive({
  lastTrace: null,
  topology: null,
  pinStage: null,
  topologyError: ''
});

function setTrace(trace) {
  if (trace) state.lastTrace = trace;
}

function setPinStage(stage) {
  state.pinStage = stage || null;
}

async function refreshTopology() {
  try {
    state.topology = await fetchTopology();
    state.topologyError = '';
  } catch (e) {
    state.topologyError = e.response?.data?.error || 'topology 無法取得';
  }
}

const flowSteps = computed(() => mergeTrace(state.lastTrace));

const displayStage = computed(() => {
  if (state.pinStage) return state.pinStage;
  if (state.topology?.inferredStage) return state.topology.inferredStage;
  return inferStage(state.topology?.services);
});

export function useDemoStoryStore() {
  return {
    state,
    flowSteps,
    displayStage,
    setTrace,
    setPinStage,
    refreshTopology
  };
}
