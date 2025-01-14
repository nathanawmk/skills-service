/*
 * Copyright 2020 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import axios from 'axios';

function buildUrl(url, params = {}) {
  let res = url;
  if (params) {
    const paramsEntries = Object.entries(params);
    if (paramsEntries && paramsEntries.length > 0) {
      const paramsStr = paramsEntries.map((entry) => `${entry[0]}=${encodeURIComponent(entry[1])}`)
        .join('&');
      res = `${res}?${paramsStr}`;
    }
  }
  return res;
}

export default {
  loadChart(projectId, metricsId, params = {}) {
    const url = buildUrl(`/admin/projects/${projectId}/metrics/${metricsId}`, params);
    return axios.get(url).then((response) => response.data);
  },
  loadGlobalMetrics(metricsId, params = {}) {
    const url = buildUrl(`/supervisor/metrics/${metricsId}`, params);
    return axios.get(url).then((response) => response.data);
  },
  loadMyMetrics(metricsId, params = {}) {
    const url = buildUrl(`/api/metrics/${metricsId}`, params);
    return axios.get(url).then((response) => response.data);
  },
};
