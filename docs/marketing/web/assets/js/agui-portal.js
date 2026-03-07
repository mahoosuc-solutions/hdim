(function () {
  var dictionaries = {
    en: {
      nav_home: 'Overview',
      nav_exec: 'Executive & Compliance',
      nav_clinical: 'Clinical Leadership',
      nav_technical: 'Technical Evaluation',
      nav_proof: 'Release Evidence',
      hero_kicker: 'HDIM AGUI PORTAL',
      hero_title: 'Healthcare automation that is event-driven, explainable, and release-verifiable.',
      hero_body: 'HDIM automates ingestion, validation, and introspection across clinical data streams so teams can see what is known, expose what is unknown, and prove operational safety before every release.',
      hero_cta_personas: 'Explore Persona Hubs',
      hero_cta_arch: 'View Platform Architecture',
      metric_1_label: 'event sources unified',
      metric_2_label: 'regulated control families',
      metric_3_label: 'release gates automated',
      metric_4_label: 'active architecture decisions',
      personas_title: 'Persona-specific pathways',
      personas_body: 'Start high-level, then go deep by stakeholder type. Every pathway maps decisions, technology, and proof artifacts.',
      persona_exec_title: 'Executive + Compliance',
      persona_exec_li1: 'Regulatory readiness posture and gap closure state',
      persona_exec_li2: 'Control-to-evidence traceability with sign-off chain',
      persona_exec_li3: 'Release go/no-go criteria and escalation steps',
      persona_exec_cta: 'Open executive hub',
      persona_clinical_title: 'Clinical Leadership',
      persona_clinical_li1: 'How care gaps are discovered, prioritized, and closed',
      persona_clinical_li2: 'Outcome instrumentation and quality measure alignment',
      persona_clinical_li3: 'AI-assisted intervention rationale and audit context',
      persona_clinical_cta: 'Open clinical hub',
      persona_technical_title: 'Technical Evaluators',
      persona_technical_li1: 'Event fabric, gateway trust, and tenant-safe orchestration',
      persona_technical_li2: 'Contract testing, runtime controls, and observability hooks',
      persona_technical_li3: 'ADR lineage for architecture and release policy decisions',
      persona_technical_cta: 'Open technical hub',
      stack_title: 'Technology and implementation stack',
      stack_body: 'The platform combines an event-based backbone, healthcare data standards, AI orchestration, and governance-by-default release operations.',
      proof_title: 'Proof of capability',
      proof_body: 'Release evidence is continuously generated and linked to controls, gap register status, and rollout decisions.',
      footer_note: 'Portal domain target: hdim-master.vercel.app | Locale: English/Spanish | Asset policy: strict provenance'
    },
    es: {
      nav_home: 'Resumen',
      nav_exec: 'Ejecutivo y Cumplimiento',
      nav_clinical: 'Liderazgo Clínico',
      nav_technical: 'Evaluación Técnica',
      nav_proof: 'Evidencia de Lanzamiento',
      hero_kicker: 'PORTAL AGUI HDIM',
      hero_title: 'Automatización de salud basada en eventos, explicable y verificable antes de cada lanzamiento.',
      hero_body: 'HDIM automatiza la ingestión, validación e introspección de datos clínicos para mostrar lo conocido, descubrir lo desconocido y demostrar seguridad operativa antes de publicar.',
      hero_cta_personas: 'Explorar áreas por perfil',
      hero_cta_arch: 'Ver arquitectura de plataforma',
      metric_1_label: 'fuentes de eventos unificadas',
      metric_2_label: 'familias de control regulatorio',
      metric_3_label: 'compuertas de lanzamiento automatizadas',
      metric_4_label: 'decisiones de arquitectura activas',
      personas_title: 'Rutas específicas por perfil',
      personas_body: 'Comience con visión general y profundice por tipo de interesado. Cada ruta conecta decisiones, tecnología y evidencia.',
      persona_exec_title: 'Ejecutivo + Cumplimiento',
      persona_exec_li1: 'Estado de preparación regulatoria y cierre de brechas',
      persona_exec_li2: 'Trazabilidad de controles a evidencia con cadena de aprobación',
      persona_exec_li3: 'Criterios de go/no-go y escalamiento de lanzamiento',
      persona_exec_cta: 'Abrir portal ejecutivo',
      persona_clinical_title: 'Liderazgo Clínico',
      persona_clinical_li1: 'Cómo se detectan, priorizan y cierran brechas de cuidado',
      persona_clinical_li2: 'Instrumentación de resultados y alineación con métricas de calidad',
      persona_clinical_li3: 'Justificación asistida por IA y contexto de auditoría',
      persona_clinical_cta: 'Abrir portal clínico',
      persona_technical_title: 'Evaluadores Técnicos',
      persona_technical_li1: 'Malla de eventos, confianza del gateway y orquestación multi-tenant',
      persona_technical_li2: 'Pruebas de contrato, controles de ejecución y observabilidad',
      persona_technical_li3: 'Línea ADR para decisiones de arquitectura y políticas de lanzamiento',
      persona_technical_cta: 'Abrir portal técnico',
      stack_title: 'Tecnología e implementación',
      stack_body: 'La plataforma combina una columna vertebral por eventos, estándares de datos clínicos, orquestación de IA y operaciones de lanzamiento con gobierno por defecto.',
      proof_title: 'Prueba de capacidad',
      proof_body: 'La evidencia de lanzamiento se genera de forma continua y se conecta con controles, estado de brechas y decisiones de despliegue.',
      footer_note: 'Dominio objetivo del portal: hdim-master.vercel.app | Idiomas: inglés/español | Política de activos: procedencia estricta'
    }
  };

  function setLanguage(lang) {
    var selected = dictionaries[lang] ? lang : 'en';
    document.documentElement.lang = selected;
    localStorage.setItem('hdim_portal_lang', selected);

    document.querySelectorAll('[data-i18n]').forEach(function (el) {
      var key = el.getAttribute('data-i18n');
      var text = dictionaries[selected][key];
      if (text) {
        el.textContent = text;
      }
    });

    document.querySelectorAll('[data-lang-btn]').forEach(function (button) {
      button.classList.toggle('active', button.getAttribute('data-lang-btn') === selected);
    });
  }

  function bindLanguageButtons() {
    document.querySelectorAll('[data-lang-btn]').forEach(function (button) {
      button.addEventListener('click', function () {
        setLanguage(button.getAttribute('data-lang-btn'));
      });
    });
  }

  function highlightCurrentPath() {
    var path = window.location.pathname;
    document.querySelectorAll('.nav-links a').forEach(function (a) {
      var href = a.getAttribute('href');
      if (!href) return;
      if (href === path || (href !== '/' && path.endsWith(href))) {
        a.classList.add('active');
      }
    });
  }

  bindLanguageButtons();
  highlightCurrentPath();
  setLanguage(localStorage.getItem('hdim_portal_lang') || 'en');
})();
