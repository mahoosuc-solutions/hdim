const fs = require('fs');

const customers = [
  { id: 'large-multi-specialty', name: 'Large Multi-Specialty Practice' },
  { id: 'regional-health-plan', name: 'Regional Health Plan' }
];

customers.forEach(customer => {
  const sourceFile = `datasets/${customer.id}-100000-fhir.json`;
  if (!fs.existsSync(sourceFile)) {
    console.log(`Skipping ${customer.id} - no source file`);
    return;
  }
  
  console.log(`Processing ${customer.name}...`);
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
    console.log(`  Created ${filename}: ${(stats.size / 1024).toFixed(1)} KB`);
  };

  createSample(data.entry.slice(0, 400), 100, `datasets/${customer.id}-100-fhir.json`);
  createSample(data.entry.slice(0, 4000), 1000, `datasets/${customer.id}-1000-fhir.json`);
});

console.log('Done!');
