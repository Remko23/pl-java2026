import React from 'react';

const MagnifyingGlassAuth = () => {
  return (
    <div className="magnifier-container">
      <div className="magnifier-lens glass neon-border-orange">
        <div className="auth-form" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100%' }}>
          <h2 className="text-neon-orange" style={{ marginBottom: '1.5rem', textAlign: 'center' }}>Keycloak Auth</h2>
          <form 
            action={async () => {
              "use server"
              const { signIn } = await import("@/auth");
              await signIn("keycloak", { redirectTo: "/dashboard" });
            }}
            style={{ display: 'flex', flexDirection: 'column', gap: '1.2rem', width: '100%' }}
          >
            <p style={{ textAlign: 'center', marginBottom: '1rem', fontSize: '0.9rem', opacity: 0.8 }}>
              Click below to authenticate via your organization's Keycloak server.
            </p>
            <button className="auth-button neon-border-orange" type="submit" style={{ cursor: 'pointer' }}>
              Sign In with Keycloak
            </button>
          </form>
        </div>
      </div>
      <div className="magnifier-handle neon-border-orange"></div>
    </div>
  );
};

export default MagnifyingGlassAuth;
