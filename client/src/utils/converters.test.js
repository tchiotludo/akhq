import { describe, expect, it } from 'vitest';
import { parseISO } from 'date-fns';
import { formatDateTime, showTime, hasPermissionForCluster, organizeRoles } from './converters.js';

describe('formatDateTime()', () => {
  let date = parseISO('2021-04-03T00:00:00.000Z');
  let value = {
    year: date.getFullYear(),
    monthValue: date.getMonth(),
    dayOfMonth: date.getDate(),
    hour: date.getHours(),
    minute: date.getMinutes(),
    second: date.getSeconds()
  };

  it('full format', () => {
    let formatted = formatDateTime(value, "yyyy-MM-dd'T'HH:mm:ss.SSSxxx");
    let formattedUtc = formatDateTime(value, "yyyy-MM-dd'T'HH:mm:ss.SSSxxx", true);
    expect(formatted).toBe('2021-04-03T02:00:00.000+02:00');
    expect(formattedUtc, 'utc').toBe('2021-04-03T00:00:00.000+00:00');
  });

  it('human readable format', () => {
    let formatted = formatDateTime(value, 'dd-MM-yyyy HH:mm');
    let formattedUtc = formatDateTime(value, 'dd-MM-yyyy HH:mm', true);
    expect(formatted).toBe('03-04-2021 02:00');
    expect(formattedUtc, 'utc').toBe('03-04-2021 00:00');
  });
});

describe('showTime()', () => {
  it('should show correct textual time', () => {
    expect(showTime(1000)).toBe('1 seconds ');
    expect(showTime(1000000)).toBe('16 minutes 40 seconds');
    expect(showTime(1000000000)).toBe('1 weeks 4 days');
    expect(showTime(36000000000)).toBe('1 years 1 months');
  });
});

describe('organizeRoles() and hasPermissionForCluster()', () => {
  const mockRoles = [
    {
      resources: ['TOPIC', 'TOPIC_DATA'],
      actions: ['READ', 'CREATE'],
      clusters: ['qa-.*', 'dev-.*']
    },
    {
      resources: ['TOPIC'],
      actions: ['READ'],
      clusters: ['prod-.*']
    }
  ];

  it('should organize roles with cluster information', () => {
    const organizedRoles = JSON.parse(organizeRoles(mockRoles));
    
    expect(organizedRoles.TOPIC).toBeDefined();
    expect(organizedRoles.TOPIC_DATA).toBeDefined();
    expect(organizedRoles.TOPIC).toHaveLength(3); // 2 from first role + 1 from second role
    expect(organizedRoles.TOPIC_DATA).toHaveLength(2); // 2 from first role only
    
    // Check structure includes cluster information
    expect(organizedRoles.TOPIC[0]).toHaveProperty('action');
    expect(organizedRoles.TOPIC[0]).toHaveProperty('clusters');
  });

  it('should correctly check permissions for specific clusters', () => {
    const organizedRoles = JSON.parse(organizeRoles(mockRoles));
    
    // Should have READ permission on qa-kafka
    expect(hasPermissionForCluster(organizedRoles, 'TOPIC', 'READ', 'qa-kafka')).toBe(true);
    
    // Should have CREATE permission on dev-cluster  
    expect(hasPermissionForCluster(organizedRoles, 'TOPIC_DATA', 'CREATE', 'dev-cluster')).toBe(true);
    
    // Should have READ permission on prod-server
    expect(hasPermissionForCluster(organizedRoles, 'TOPIC', 'READ', 'prod-server')).toBe(true);
    
    // Should NOT have CREATE permission on prod-server (only READ allowed)
    expect(hasPermissionForCluster(organizedRoles, 'TOPIC', 'CREATE', 'prod-server')).toBe(false);
    
    // Should NOT have any permission on staging-cluster (no matching pattern)
    expect(hasPermissionForCluster(organizedRoles, 'TOPIC', 'READ', 'staging-cluster')).toBe(false);
    
    // Should NOT have permission for non-existent resource
    expect(hasPermissionForCluster(organizedRoles, 'NONEXISTENT', 'READ', 'qa-kafka')).toBe(false);
  });

  it('should maintain backward compatibility with old format', () => {
    const oldFormatRoles = {
      TOPIC: ['READ', 'CREATE']
    };
    
    // Should work with old string-based format
    expect(hasPermissionForCluster(oldFormatRoles, 'TOPIC', 'READ', 'any-cluster')).toBe(true);
    expect(hasPermissionForCluster(oldFormatRoles, 'TOPIC', 'DELETE', 'any-cluster')).toBe(false);
  });
});
