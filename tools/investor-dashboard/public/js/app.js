document.addEventListener('DOMContentLoaded', () => {
  // Pipeline funnel chart
  if (typeof funnelData !== 'undefined' && document.getElementById('funnelChart')) {
    const ctx = document.getElementById('funnelChart').getContext('2d');
    const stages = Object.keys(funnelData);
    const counts = Object.values(funnelData);
    const colors = [
      '#6c757d', '#0d6efd', '#0dcaf0', '#ffc107',
      '#fd7e14', '#198754', '#20c997', '#6610f2', '#dc3545'
    ];
    new Chart(ctx, {
      type: 'bar',
      data: {
        labels: stages,
        datasets: [{
          label: 'Contacts',
          data: counts,
          backgroundColor: colors.slice(0, stages.length),
          borderRadius: 4,
        }]
      },
      options: {
        indexAxis: 'y',
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          x: { beginAtZero: true, ticks: { stepSize: 1 } }
        }
      }
    });
  }

  // Type breakdown pie chart
  if (typeof typeData !== 'undefined' && document.getElementById('typeChart')) {
    const ctx = document.getElementById('typeChart').getContext('2d');
    new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: Object.keys(typeData),
        datasets: [{
          data: Object.values(typeData),
          backgroundColor: ['#0d6efd', '#ffc107', '#198754']
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
      }
    });
  }

  // Copy-to-clipboard functionality
  document.querySelectorAll('.copy-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const targetId = btn.dataset.copyTarget;
      const el = document.getElementById(targetId);
      if (!el) return;
      const text = el.innerText || el.value;
      navigator.clipboard.writeText(text).then(() => {
        // Store original content and show confirmation
        const originalText = btn.textContent;
        btn.textContent = 'Copied!';
        btn.classList.add('copied');
        setTimeout(() => {
          btn.textContent = originalText;
          btn.classList.remove('copied');
        }, 2000);
      });
    });
  });
});
