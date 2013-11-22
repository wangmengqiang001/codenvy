/*
 *
 * CODENVY CONFIDENTIAL
 * ________________
 *
 * [2012] - [2013] Codenvy, S.A.
 * All Rights Reserved.
 * NOTICE: All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any. The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */

IMPORT 'macros.pig';

f1 = loadResources('$LOG', '$FROM_DATE', '$TO_DATE', '$USER', '$WS');
f = filterByEvent(f1, 'project-deployed,application-created');

a1 = extractParam(f, 'TYPE', 'type');
a2 = extractParam(a1, 'PROJECT', 'project');
a3 = extractParam(a2, 'PAAS', 'paas');
a = FOREACH a3 GENERATE dt, ws, user, project, type, paas;

a = DISTINCT a4;


r3 = GROUP r2 BY paas;
result = FOREACH r3 GENERATE group, COUNT(r2);



f1 = filterByEvent(l, '$EVENT');
f = extractParam(f1, '$PARAM', param);

a1 = FOREACH f GENERATE LOWER(param) AS param, event;
a2 = GROUP a1 BY param;
a = FOREACH a2 GENERATE group AS param, COUNT(a1) AS countAll;

result = FOREACH a GENERATE ToMilliSeconds(ToDate('$TO_DATE', 'yyyyMMdd')), TOTUPLE(param, countAll);
STORE result INTO '$STORAGE_URL.$STORAGE_TABLE' USING MongoStorage();

r1 = FOREACH f GENERATE dt, ws, user, LOWER(REGEX_EXTRACT(user, '.*@(.*)', 1)) AS domain, LOWER(param) AS param;
r = FOREACH r1 GENERATE ToMilliSeconds(dt), TOTUPLE('ws', ws), TOTUPLE('user', user), TOTUPLE('domain', domain), TOTUPLE(param, 1L);
STORE r INTO '$STORAGE_URL.$STORAGE_TABLE-raw' USING MongoStorage();

