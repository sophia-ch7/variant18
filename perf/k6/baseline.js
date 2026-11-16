import { thresholds, userFlow } from './common.js';

export const options = {
  scenarios: {
    baseline: { executor: 'constant-vus', vus: 1, duration: '1m' },
  },
  thresholds,
};

export default function () {
  userFlow();
}