import React from 'react';

const Pixel8Mockup = ({ children, screenColor = '#121212' }: { children?: React.ReactNode; screenColor?: string }) => {

  return (
    <div className="p8-wrapper">
      <style>{`
        .p8-wrapper {
          position: relative;
          width: 328px;
          height: 690px;
          margin: 40px auto;
          font-family: 'Inter', sans-serif;
          user-select: none;
        }

        .p8-frame {
          position: absolute;
          left: 4px;
          width: 320px;
          height: 680px;
          background: #202124;
          border-radius: 32px;
          border: 3px solid #3c4043;
          box-shadow: 0 30px 60px -12px rgba(0, 0, 0, 0.45);
          overflow: hidden;
          z-index: 2;
        }

        .p8-button {
          position: absolute;
          right: 1px; 
          width: 4px;
          background: #444;
          border-radius: 0 2px 2px 0;
          z-index: 1;
          box-shadow: 1px 0 2px rgba(0,0,0,0.2);
        }
        .p8-power { top: 110px; height: 42px; } 
        .p8-volume { top: 172px; height: 88px; }

        .p8-screen {
          position: absolute;
          inset: 6px; 
          background: ${screenColor};
          border-radius: 28px;
          display: flex;
          flex-direction: column;
          overflow: hidden;
        }

        .p8-camera {
          position: absolute;
          top: 16px;
          left: 50%;
          transform: translateX(-50%);
          width: 14px;
          height: 14px;
          background: #000;
          border-radius: 50%;
          border: 1px solid #222;
          z-index: 50;
        }

        .p8-status-bar {
          height: 44px;
          padding: 0 24px;
          display: flex;
          justify-content: flex-end;
          align-items: center;
          color: #fff;
          font-size: 13px;
          font-weight: 500;
          z-index: 40;
        }

        .p8-content {
          flex: 1;
          width: 100%;
          position: relative;
        }
        .p8-content img {
          width: 100%;
          height: 100%;
          object-fit: cover;
        }

        .p8-nav-bar {
          height: 20px;
          display: flex;
          justify-content: center;
          align-items: flex-start;
          padding-top: 4px;
        }
        .p8-pill {
          width: 60px;
          height: 3px;
          background: rgba(255, 255, 255, 0.5);
          border-radius: 2px;
        }
      `}</style>

      <div className="p8-button p8-power"></div>
      <div className="p8-button p8-volume"></div>

      <div className="p8-frame">
        <div className="p8-camera"></div>
        
        <div className="p8-screen">
          <div className="p8-status-bar">
          </div>

          <div className="p8-content">
            {children ? children : (
              <div style={{color: '#444', display: 'grid', placeContent: 'center', height: '100%', fontSize: '13px'}}>
                Pixel 8 Content
              </div>
            )}
          </div>

          <div className="p8-nav-bar">
            <div className="p8-pill"></div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Pixel8Mockup;
