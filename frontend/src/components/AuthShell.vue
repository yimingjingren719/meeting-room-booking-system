<script setup>
import { CalendarCheck2, MapPin } from '@lucide/vue'
</script>

<template>
  <main class="auth-shell">
    <section class="auth-visual">
      <div class="visual-overlay" />
      <div class="visual-brand">
        <span class="visual-mark"><CalendarCheck2 :size="24" /></span>
        <span>
          <strong>USST Meeting Hub</strong>
          <small>会议室预约系统</small>
        </span>
      </div>
      <div class="visual-copy">
        <p class="visual-kicker">上海理工大学 · 军工路校区</p>
        <h1>统一管理校内会议空间<br />让每一次预约都有据可查</h1>
        <p class="visual-address">
          <MapPin :size="16" />
          光电信息与计算机工程学院
        </p>
      </div>
    </section>

    <section class="auth-content">
      <div class="auth-panel">
        <slot />
      </div>
      <p class="auth-footer">2026 计算机项目综合开发创新实践</p>
    </section>
  </main>
</template>

<style scoped>
.auth-shell {
  display: grid;
  grid-template-columns: minmax(420px, 1.05fr) minmax(430px, 0.95fr);
  min-height: 100vh;
  background: #f7f8fa;
}

.auth-visual {
  position: relative;
  display: flex;
  min-height: 100vh;
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;
  padding: 38px 46px;
  background:
    linear-gradient(90deg, rgba(13, 24, 44, 0.92), rgba(13, 24, 44, 0.48)),
    url("/rooms/room-508.jpg")
      center/cover;
  color: white;
}

.visual-overlay {
  position: absolute;
  inset: auto 0 0;
  height: 42%;
  background: linear-gradient(0deg, rgba(8, 16, 30, 0.8), transparent);
}

.visual-brand,
.visual-copy {
  position: relative;
  z-index: 1;
}

.visual-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.visual-mark {
  display: grid;
  width: 43px;
  height: 43px;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: 9px;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(10px);
}

.visual-brand > span:last-child {
  display: grid;
}

.visual-brand strong {
  font-size: 15px;
}

.visual-brand small {
  margin-top: 2px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 11px;
}

.visual-copy {
  max-width: 610px;
  padding-bottom: 46px;
}

.visual-kicker {
  margin: 0 0 16px;
  color: #8fc4ff;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.visual-copy h1 {
  margin: 0;
  font-size: clamp(31px, 3.2vw, 50px);
  font-weight: 760;
  letter-spacing: -0.02em;
  line-height: 1.2;
}

.visual-address {
  display: flex;
  align-items: center;
  gap: 7px;
  margin: 24px 0 0;
  color: rgba(255, 255, 255, 0.72);
  font-size: 13px;
}

.auth-content {
  display: grid;
  min-height: 100vh;
  place-items: center;
  padding: 38px;
}

.auth-panel {
  width: min(430px, 100%);
}

.auth-footer {
  position: absolute;
  bottom: 18px;
  margin: 0;
  color: #98a2b3;
  font-size: 11px;
}

@media (max-width: 900px) {
  .auth-shell {
    display: block;
  }

  .auth-visual {
    min-height: 230px;
    padding: 24px;
  }

  .visual-copy {
    padding: 50px 0 10px;
  }

  .visual-copy h1 {
    font-size: 28px;
  }

  .visual-kicker,
  .visual-address {
    display: none;
  }

  .auth-content {
    min-height: calc(100vh - 230px);
    padding: 28px 18px 52px;
  }
}
</style>

