import { createRouter, createWebHashHistory } from 'vue-router'
import HomePage from './pages/HomePage.vue'
import CustomersPage from './pages/CustomersPage.vue'
import AboutPage from './pages/AboutPage.vue'
import AnalyticsPage from './pages/AnalyticsPage.vue'
const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', component: HomePage, meta: { title: 'Огляд' } },
    { path: '/customers', component: CustomersPage, meta: { title: 'Клієнти' } },
    { path: '/analytics', component: AnalyticsPage, meta: { title: 'BI аналітика' } },
    { path: '/about', component: AboutPage, meta: { title: 'Про проєкт' } },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ],
  scrollBehavior: () => ({ top: 0 })
})
router.afterEach(to => { document.title = `${to.meta.title} · BankContact` })
export default router
