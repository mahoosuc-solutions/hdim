const path = require('path');
const fs = require('fs');

describe('load-test-patients — manifest validation', () => {
  const manifestPath = path.join(__dirname, '..', 'manifest.json');

  it('manifest exists and is valid JSON', () => {
    const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8'));
    expect(manifest.phenotypes).toBeInstanceOf(Array);
    expect(manifest.phenotypes.length).toBe(6);
  });

  it('every phenotype references existing bundle and overlay files', () => {
    const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8'));
    for (const p of manifest.phenotypes) {
      const bundlePath = path.join(__dirname, '..', p.bundle);
      const overlayPath = path.join(__dirname, '..', p.overlay);
      expect(fs.existsSync(bundlePath)).toBe(true);
      expect(fs.existsSync(overlayPath)).toBe(true);
    }
  });

  it('every bundle is valid FHIR R4 transaction Bundle', () => {
    const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8'));
    for (const p of manifest.phenotypes) {
      const bundle = JSON.parse(fs.readFileSync(path.join(__dirname, '..', p.bundle), 'utf8'));
      expect(bundle.resourceType).toBe('Bundle');
      expect(bundle.type).toBe('transaction');
      expect(bundle.entry).toBeInstanceOf(Array);
      expect(bundle.entry.length).toBeGreaterThan(0);
      for (const entry of bundle.entry) {
        expect(entry.resource).toBeDefined();
        expect(entry.resource.resourceType).toBeDefined();
        expect(entry.request).toBeDefined();
        expect(entry.request.method).toBeDefined();
        expect(entry.request.url).toBeDefined();
      }
    }
  });

  it('every bundle has a Patient resource', () => {
    const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8'));
    for (const p of manifest.phenotypes) {
      const bundle = JSON.parse(fs.readFileSync(path.join(__dirname, '..', p.bundle), 'utf8'));
      const patients = bundle.entry.filter(e => e.resource.resourceType === 'Patient');
      expect(patients.length).toBeGreaterThanOrEqual(1);
    }
  });

  it('every overlay references its phenotype', () => {
    const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8'));
    for (const p of manifest.phenotypes) {
      const overlay = JSON.parse(fs.readFileSync(path.join(__dirname, '..', p.overlay), 'utf8'));
      expect(overlay.phenotypeId).toBe(p.id);
    }
  });
});
