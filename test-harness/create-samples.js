const fs = require('fs');

const sourceFile = 'datasets/academic-medical-center-5000-fhir.json';
console.log('Reading source file...');
const data = JSON.parse(fs.readFileSync(sourceFile, 'utf8'));

const createSample = (entries, size, filename) => {
  const sample = {
    resourceType: data.resourceType,
    type: data.type,
    meta: { ...data.meta, sampleSize: `${size} patients` },
    entry: entries
  };
  fs.writeFileSync(filename, JSON.stringify(sample, null, 2));
  const stats = fs.statSync(filename);
  console.log(`Created ${filename}: ${(stats.size / 1024).toFixed(1)} KB`);
};

// Each patient has ~4 resources
createSample(data.entry.slice(0, 400), 100, 'datasets/academic-medical-center-100-fhir.json');
createSample(data.entry.slice(0, 4000), 1000, 'datasets/academic-medical-center-1000-fhir.json');

console.log('Done!');
