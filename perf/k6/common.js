import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE_URL || 'http://127.0.0.1:8080';
const JSON_HEADERS = { 'Content-Type': 'application/json' };

export const thresholds = {
  http_req_duration: ['p(95)<500'],
  http_req_failed: ['rate<0.01'],
};

export function userFlow(thinkTime = 1) {
  const uid = `${Date.now()}-${__VU}-${__ITER}`;

  // 1. Створити слухача
  const studentRes = http.post(
    `${BASE}/students`,
    JSON.stringify({ code: `S-${uid}`, age: 20, entryScore: 90, regular: false }),
    { headers: JSON_HEADERS, tags: { name: 'POST /students' } }
  );
  if (!check(studentRes, { 'student 201': (r) => r.status === 201 })) return;
  const studentId = studentRes.json('id');

  // 2. Створити курс
  const courseRes = http.post(
    `${BASE}/courses`,
    JSON.stringify({ code: `C-${uid}`, price: 100000, capacity: 10, level: 1 }),
    { headers: JSON_HEADERS, tags: { name: 'POST /courses' } }
  );
  if (!check(courseRes, { 'course 201': (r) => r.status === 201 })) return;
  const courseId = courseRes.json('id');

  // 3. Зарахувати слухача на курс (обидва id з попередніх відповідей)
  const enrollRes = http.post(
    `${BASE}/enrollments`,
    JSON.stringify({ reference: `E-${uid}`, studentId: studentId, courseId: courseId, couponValid: false }),
    { headers: JSON_HEADERS, tags: { name: 'POST /enrollments' } }
  );
  if (!check(enrollRes, { 'enrollment 201': (r) => r.status === 201 })) return;
  const enrollmentId = enrollRes.json('id');

  // 4. Прочитати заявку за id з попередньої відповіді
  const getRes = http.get(`${BASE}/enrollments/${enrollmentId}`, {
    tags: { name: 'GET /enrollments/{id}' },
  });
  check(getRes, { 'get 200': (r) => r.status === 200 });

    if (thinkTime > 0) sleep(thinkTime);
}