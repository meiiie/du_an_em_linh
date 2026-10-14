self.addEventListener('push', event => {
  event.waitUntil(self.registration.showNotification('MathL+ · Nhắc học', {
    body: 'Em có lịch học hoặc hạn nộp sắp tới.', tag: 'mathl-nhac-hoc', data: {url: '/hs/lich'},
  }));
});
self.addEventListener('notificationclick', event => {
  event.notification.close();
  event.waitUntil(self.clients.matchAll({type: 'window', includeUncontrolled: true}).then(async clients => {
    const client = clients.find(c => new URL(c.url).origin === self.location.origin);
    if (client) { await client.navigate('/hs/lich'); return client.focus(); }
    return self.clients.openWindow('/hs/lich');
  }));
});
