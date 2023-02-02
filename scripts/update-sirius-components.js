/*******************************************************************************
 * Copyright (c) 2022 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
const childProcess = require('child_process');
const path = require('path');
const fs = require('fs');

const newSiriusComponentsVersion = process.argv[2];

if (!newSiriusComponentsVersion) {
  console.log('Use this script like this:');
  console.log('node scripts/update-sirius-components.js 2022.3.0');
  process.exit(1);
}

const workspace = process.cwd();

const projects = [
  'sirius-web-frontend',
  'sirius-web-persistence',
  'sirius-web-services-api',
  'sirius-web-services',
  'sirius-web-graphql-schema',
  'sirius-web-graphql',
  'sirius-web-spring',
  'sirius-web-sample-application'
];

console.log('Updating the following pom.xml:');
for (let index = 0; index < projects.length; index++) {
  const project = projects[index];
  const pomXmlPath = path.join(workspace, 'backend', project, 'pom.xml');
  console.log(pomXmlPath);

  const pomXmlContent = fs.readFileSync(pomXmlPath, { encoding: 'utf-8' });
  const startTagIndex = pomXmlContent.indexOf('<sirius.components.version>');
  const endTagIndex = pomXmlContent.indexOf('</sirius.components.version>');
  if (startTagIndex !== -1 && endTagIndex !== -1) {
    let newPomXmlContent = pomXmlContent.substring(0, startTagIndex + '<sirius.components.version>'.length);
    newPomXmlContent += newSiriusComponentsVersion;
    newPomXmlContent += pomXmlContent.substring(endTagIndex);
    fs.writeFileSync(pomXmlPath, newPomXmlContent, { encoding: 'utf-8' });
  }
}

const updateSiriusComponentsCommand = `npm install @eclipse-sirius/sirius-components@${newSiriusComponentsVersion} @eclipse-sirius/sirius-components-charts@${newSiriusComponentsVersion} @eclipse-sirius/sirius-components-core@${newSiriusComponentsVersion} @eclipse-sirius/sirius-components-diagrams@${newSiriusComponentsVersion} @eclipse-sirius/sirius-components-forms@${newSiriusComponentsVersion} @eclipse-sirius/sirius-components-formdescriptioneditors@${newSiriusComponentsVersion} @eclipse-sirius/sirius-components-selection@${newSiriusComponentsVersion} @eclipse-sirius/sirius-components-trees@${newSiriusComponentsVersion} @eclipse-sirius/sirius-components-validation@${newSiriusComponentsVersion} --save-exact`;

console.log('Updating @eclipse-sirius/sirius-components in the frontend');
const frontendWorkingDirectory = path.join(workspace, 'frontend');
childProcess.execSync(updateSiriusComponentsCommand, { cwd: frontendWorkingDirectory , stdio: 'inherit' });

const gitAddCommand = `git add backend frontend`;
console.log(gitAddCommand);
childProcess.execSync(gitAddCommand, { stdio: 'inherit' });

const gitCommitCommand = `git commit -s -m "[releng] Switch to Sirius Components ${newSiriusComponentsVersion}"`;
console.log(gitCommitCommand);
childProcess.execSync(gitCommitCommand, { stdio: 'inherit' });