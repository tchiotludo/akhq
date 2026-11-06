import { describe, expect, it, vi } from 'vitest';
import { Acls } from './Acls';

const buildAcls = () => {
  const acls = new Acls({
    location: { search: '' },
    params: { clusterId: 'local' },
    router: { navigate: vi.fn() }
  });

  acls.setState = state => {
    acls.state = { ...acls.state, ...state };
  };

  return acls;
};

describe('Acls', () => {
  it('encodes ACL principals as UTF-8 Base64 without throwing for Unicode values', () => {
    const acls = buildAcls();
    const data = [
      { principal: 'User:alice' },
      { principal: 'User:张伟' },
      { principal: 'User:Renée' },
      { principal: 'User:प्रिय' },
      { principal: 'User:service/account+ops' },
      { principal: '' }
    ];

    expect(() => acls.handleData(data)).not.toThrow();
    expect(data.map(acl => acl.principalEncoded)).toEqual([
      'VXNlcjphbGljZQ==',
      'VXNlcjrlvKDkvJ8=',
      'VXNlcjpSZW7DqWU=',
      'VXNlcjrgpKrgpY3gpLDgpL/gpK8=',
      'VXNlcjpzZXJ2aWNlL2FjY291bnQrb3Bz',
      ''
    ]);
  });
});
